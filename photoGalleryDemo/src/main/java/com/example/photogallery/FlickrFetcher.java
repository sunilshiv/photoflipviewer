package com.example.photogallery;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.net.Uri;
import android.util.Log;

public class FlickrFetcher {
	
	private static final String TAG = "FlickrFetcher";
	
	private static final String ENDPOINT = "https://api.flickr.com/services/rest";
	private static final String API_KEY = "778ea949efbad8c6a003c65f5af27eb0";
	private static final String METHOD_GETRECENT = "flickr.photos.getRecent";
	private static final String PARAMS_EXTRA = "extras";
	
	private static final String EXTRA_SMALL_URL = "url_s";
	private static final String XML_PHOTO = "photo";

	public byte[] getUrlBytes(String urlSpec) throws IOException
	{
		URL url = new URL(urlSpec);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			InputStream in = connection.getInputStream();
			
			int code = connection.getResponseCode();
			if (code != HttpURLConnection.HTTP_OK) {
				Log.e(TAG, "response code as <" + code + ">");
				return null;//
			}
			
			Log.i(TAG, "response code as <" + code + ">");
			
			int bytesRead = 0;
			byte[] buffer = new byte[1024];
			while ((bytesRead = in.read(buffer)) > 0) {
				out.write(buffer, 0, bytesRead);
			}
			out.close();

			return out.toByteArray();	
		} finally
		{
			connection.disconnect();
		}
	}
	
	public String getUrl(String urlSpec) throws IOException
	{
		return new String(getUrlBytes(urlSpec));
	}
	
	/*
	 * construct request url and get return xml data of flickr photos
	 * then parse xml by converting to local gallery items
	 */
	public ArrayList<GalleryItem> fetchItems()
	{
		
		ArrayList<GalleryItem> items = new ArrayList<GalleryItem>();
		
		String url = Uri.parse(ENDPOINT).buildUpon()
				.appendQueryParameter("method", METHOD_GETRECENT)
				.appendQueryParameter("api_key", API_KEY)
				.appendQueryParameter(PARAMS_EXTRA, EXTRA_SMALL_URL)
				.build().toString();
		
		try {
			Log.i(TAG, "url as <" + url + ">");
			String xmlString = getUrl(url);
			
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			XmlPullParser parser = factory.newPullParser();
			parser.setInput(new StringReader(xmlString));
			
			parseItems(items, parser);
			
			Log.i(TAG, "Received xml: " + xmlString);
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG, "Failed to fetch items", e);
		} catch (XmlPullParserException e) {
			e.printStackTrace();
			Log.e(TAG, "Failed to parse items", e);
		}
		
		return items;
	}
	
	/*
	 * part passed xml data and convert local gallery items list meta-data
	 */
	public void parseItems(ArrayList<GalleryItem> items, XmlPullParser parser) throws XmlPullParserException, IOException
	{
		int eventType = parser.next();
		
		while (eventType != XmlPullParser.END_DOCUMENT) {
			if ( eventType == XmlPullParser.START_TAG 
					&& XML_PHOTO.equals(parser.getName())) {
				String id = parser.getAttributeValue(null, "id");
				String caption = parser.getAttributeValue(null, "title");
				String smallUrl = parser.getAttributeValue(null, EXTRA_SMALL_URL);
				
				GalleryItem item = new GalleryItem();
				item.setId(id);
				item.setCaption(caption);
				item.setUrl(smallUrl);
				items.add(item);
			}
			
			eventType = parser.next();
		}
	}
}
