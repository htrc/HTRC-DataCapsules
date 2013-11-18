package edu.indiana.d2i.sloan.bean;

public class ImageInfoBean {
	private String imageName;
	private String imageDescription;

	public ImageInfoBean(String imageName, String imageDescription) {
		this.imageName = imageName;
		this.imageDescription = imageDescription;
	}
	
	public String getImageName() {
		return imageName;
	}
	
	public String getImageDescription() {
		return imageDescription;
	}
}
