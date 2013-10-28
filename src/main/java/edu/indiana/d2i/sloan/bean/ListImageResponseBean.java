package edu.indiana.d2i.sloan.bean;

import java.util.*;

public class ListImageResponseBean {
	private List<ImageInfoBean> images = new ArrayList<ImageInfoBean>();
	
	public ListImageResponseBean(List<ImageInfoBean> images) {
		this.images = images;
	}
	
	public List<ImageInfoBean> getImageInfo() {
		return images;
	}
	
	@Override
	public String toString() {
		return images.toString();
	}
}
