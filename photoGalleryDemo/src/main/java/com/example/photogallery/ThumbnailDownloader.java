package com.example.photogallery;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.media.session.MediaSessionCompat.Token;
import android.support.v4.util.LruCache;
import android.util.Log;

public class ThumbnailDownloader<Token> extends HandlerThread {
	
	private static final String TAG = "ThumbnailDownloader";
	private static final int MESSAGE_DOWNLOAD = 0;
	
	Handler mHandler;//relates to downloader thread
	Handler mResponseHandler;//from main thread
    Listener<Token> mListener;
    
    LruCache<String, Bitmap> mCache;
    
    public interface Listener<Token> {
    	void onThumbnailDownloaded(Token token, Bitmap bitmap);
    }
    
    public void setListener(Listener<Token> listener)
    {
    	mListener = listener;
    }
	
	Map<Token, String> requestMap = Collections.synchronizedMap(new HashMap<Token, String>());

	public ThumbnailDownloader(Handler responseHandler, LruCache<String, Bitmap> cache) {
		super(TAG);
		mResponseHandler = responseHandler;
		mCache = cache;
	}
	
	public void queueThumbnail(Token token, String url)
	{
		Log.i(TAG, "Got a url :" + url);
		requestMap.put(token, url);
		
		mHandler.obtainMessage(MESSAGE_DOWNLOAD, token).sendToTarget();
	}
	
	@Override
	protected void onLooperPrepared() {
		mHandler = new Handler()
		{
			@Override
			public void handleMessage(Message msg) {
				if (msg.what == MESSAGE_DOWNLOAD) {
					
					@SuppressWarnings("unchecked")
					Token token = (Token) msg.obj;
					Log.i(TAG, "Got a request for url : " + requestMap.get(token));
					handleRequest(token);
				}
			}
		};
	}
	
	private void handleRequest(final Token token)
	{
		final String url = requestMap.get(token);
		if (url == null) {
			return;
		}
		
		byte[] bitmapBytes;
		try {
			bitmapBytes = new FlickrFetcher().getUrlBytes(url);
			
			final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
			
			mResponseHandler.post(new Runnable() {
				
				@Override
				public void run() {
					if (requestMap.get(token) != url) {
						return;
					}
					
					//download completed! then remove from map
					requestMap.remove(token);
					
					//add to cache if previously not
					if (mCache != null) {
						if (mCache.get(url) == null) {
							//previously not added, then we should add to cache
							mCache.put(url, bitmap);
						}
						else {
							
							//previously added, then do nothing
							
							if (mCache.get(url) != bitmap) {
								Log.e(TAG, "New bitmap linked!!");
							}
						}
					}
					else {
						Log.e(TAG, "Cache is not existing...");
					}
					
					mListener.onThumbnailDownloaded(token, bitmap);
				}
			});
			
			Log.i(TAG, "Bitmap Created!");
		} catch (IOException e) {
			Log.e(TAG, "Error downloading image", e);
			e.printStackTrace();
		}
	}
	
	public void clearQueue()
	{
		requestMap.clear();
		mHandler.removeMessages(MESSAGE_DOWNLOAD);
	}
}
