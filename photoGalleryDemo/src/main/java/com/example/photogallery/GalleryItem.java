package com.example.photogallery;

public class GalleryItem {

	private String mCaption;
	private String mId;
	private String mUrl;
	private int mCount;
	public int getmCount() {
		return mCount;
	}

	public void setmCount(int mCount) {
		this.mCount = mCount;
	}


	
	@Override
	public String toString() {
		return mCaption;
	}

	public String getCaption() {
		return mCaption;
	}

	public void setCaption(String caption) {
		mCaption = caption;
	}

	public String getId() {
		return mId;
	}

	public void setId(String id) {
		mId = id;
	}

	public String getUrl() {
		return mUrl;
	}

	public void setUrl(String url) {
		mUrl = url;
	}
	
	
}
