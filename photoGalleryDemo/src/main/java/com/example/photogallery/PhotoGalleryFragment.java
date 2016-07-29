package com.example.photogallery;

import java.util.ArrayList;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.util.LruCache;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;
import android.widget.ViewFlipper;

public class PhotoGalleryFragment extends Fragment {
	private static final String TAG = "PhotoGalleryFragment";
	private GridView mGridView;
	private ArrayList<GalleryItem> mItems;
	private Handler mHandler;
	
	private ThumbnailDownloader<ImageView> mThumbnailThread;
	
	//cache to store all bitmaps according to LRU policy
	private LruCache<String, Bitmap> mCache;
	ImageView imageView;

	boolean isBackVisible = false;

	private TextView tv;
	private TextView description;
	private int count;
	private GalleryItem item;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);

		
		//decide memory for cache
		int maxMemory = (int) Runtime.getRuntime().maxMemory();
		int cacheSize = maxMemory / 8;
		
		mCache = new LruCache<String, Bitmap>(cacheSize)
		{
			@SuppressLint("NewApi")
			@Override
			protected int sizeOf(String key, Bitmap value) {
				// TODO Auto-generated method stub
				return value.getByteCount();
			}
		};
		
		new FetchItemsTask().execute();
		
		mHandler = new Handler();
		mThumbnailThread = new ThumbnailDownloader<ImageView>(mHandler, mCache);
		mThumbnailThread.setListener(new ThumbnailDownloader.Listener<ImageView>() {

			@Override
			public void onThumbnailDownloaded(ImageView token, Bitmap bitmap) {
				
				if (isVisible()) {
					token.setImageBitmap(bitmap);
				}
			}
			
		});
		
		mThumbnailThread.start();
		mThumbnailThread.getLooper();
		Log.i(TAG, "Background thread started!");
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		mThumbnailThread.quit();
		Log.i(TAG, "Background thread destroyed!");
	}
	
	@Override
	public void onDestroyView() {
		// TODO Auto-generated method stub
		super.onDestroyView();
		mThumbnailThread.clearQueue();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
		
		mGridView = (GridView) view.findViewById(R.id.gridview);

		//we should setup this again within onCreateView as each time rotation will lead to resize of fragment layout
		setupAdapter();
		return view;
	}
	
	private void setupAdapter() {
		if (getActivity() == null || mGridView == null) {
			//check if the activity this fragment attached with is still existing as we will create adapter for now
			//and this is very important if null, it will lead to a crash of app
			return;
		}
		
		Log.i(TAG, "Activity and Gridview all live");
		
		if (mItems != null) {
			//mGridView.setAdapter(new ArrayAdapter<GalleryItem>(getActivity(), android.R.layout.simple_gallery_item, mItems));
			mGridView.setAdapter(new GalleryAdapter(mItems));
			mGridView.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					if(v.isSelected()){
						count++;
						item.setmCount(count);
					}else{
						count--;
						item.setmCount(count);
					}

					return false;
				}
			});

			/*mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					Toast.makeText(getContext(),"Id clicked"+mItems.get(position).getId(),Toast.LENGTH_SHORT).show();

					tv.setText(mItems.get(position).getId().toString());
					flipTheView(view);
					final AnimatorSet setRightOut = (AnimatorSet) AnimatorInflater.loadAnimator(getContext(),
							R.animator.card_flip_right_in);


					final AnimatorSet setLeftIn = (AnimatorSet) AnimatorInflater.loadAnimator(getContext(),
							R.animator.card_flip_right_out);

					*//*if(!isBackVisible){
						setRightOut.setTarget(imageView);
						setLeftIn.setTarget(tv);
						setRightOut.start();
						setLeftIn.start();
						isBackVisible = true;
					}
					else{
						setRightOut.setTarget(tv);
						setLeftIn.setTarget(imageView);
						setRightOut.start();
						setLeftIn.start();
						isBackVisible = false;
					}*//*




				}
			});*/
		}
		else {
			Log.e(TAG, "no data!!");
			mGridView.setAdapter(null);
		}
		//
	}

	private void flipTheView(View view) {
		isBackVisible = true;
		AnimatorSet setFlipInFront = (AnimatorSet) AnimatorInflater.loadAnimator(getContext(), R.animator.flip_in_front);

		setFlipInFront.setTarget(view);
		setFlipInFront.start();
	}
	
	private class GalleryAdapter extends ArrayAdapter<GalleryItem>
	{	
		
		public GalleryAdapter(ArrayList<GalleryItem> items) {
			super(getActivity(), 0, items);

		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = getActivity().getLayoutInflater().inflate(R.layout.gallery_item, parent, false);
			}
			final ViewFlipper flipper = (ViewFlipper) convertView.findViewById(R.id.my_view_flipper);
			imageView = (ImageView) convertView.findViewById(R.id.gallery_item_imageview);
			tv = (TextView)convertView.findViewById(R.id.textView);
			description = (TextView)convertView.findViewById(R.id.textViewDesc);
			
			item = getItem(position);
			
			//check if any picture stored so that we can get picture directly
			if (!mCache.equals("")&& !item.equals("") && mCache != null && item != null) {
				
				Log.i(TAG, "Cache is living and item is existing...");
				
				String url = item.getUrl();
				String id = item.getId();
				String desc = item.getCaption();
				
				if (mCache.get(url) != null) {
					imageView.setImageBitmap(mCache.get(url));
					tv.setText(id);
					description.setText(desc);
				} else {

					Log.i(TAG, "Cache doest no have this url :" + url + " related bitmap data!");

					//if no cache, then we should defaultly put on a startup image for all view
					imageView.setImageResource(R.drawable.ic_launcher);
					mThumbnailThread.queueThumbnail(imageView, item.getUrl());
					tv.setText(item.getId());
					description.setText(desc);
				}

				flipper.setOnClickListener(new View.OnClickListener(){
					@Override
					public void onClick(View click) {

						flipViewFlipper(flipper);

					}

				});

			} else {
				Log.e(TAG, "mCache is not existing...");
			}

			return convertView;
		}
		
	}

	private void flipViewFlipper(ViewFlipper flipper){

		if(flipper.getDisplayedChild() == 0){
			AnimatorSet setFlipInFront = (AnimatorSet) AnimatorInflater.loadAnimator(getContext(), R.animator.card_flip_left_in);
			setFlipInFront.setTarget(flipper);
			setFlipInFront.start();
			flipper.setDisplayedChild(1);
			//
			//

		}
		else{
			AnimatorSet setFlipInFront = (AnimatorSet) AnimatorInflater.loadAnimator(getContext(), R.animator.card_flip_right_in);
			setFlipInFront.setTarget(flipper);
			setFlipInFront.start();
			flipper.setDisplayedChild(0);
		}

	}

	private class FetchItemsTask extends AsyncTask<Void, Void, ArrayList<GalleryItem>>
	{

		@Override
		protected ArrayList<GalleryItem> doInBackground(Void... params) {

//			try {
//				String result = new FlickrFetcher().getUrl("http://www.baidu.com");
//				Log.i(TAG, "Fetched contents of URL" + result);
//			} catch (IOException e) {
//				e.printStackTrace();
//				Log.e(TAG, "Failed to fetch URL :", e);
//			}
			
			return new FlickrFetcher().fetchItems();
		}
		
		@Override
		protected void onPostExecute(ArrayList<GalleryItem> items) {
			Log.i(TAG, "fetch completed!");
			mItems = items;
			setupAdapter();
		}
		
	}
}
