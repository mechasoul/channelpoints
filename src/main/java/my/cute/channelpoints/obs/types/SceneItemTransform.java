package my.cute.channelpoints.obs.types;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class SceneItemTransform {

	@SerializedName("position.x")
	private double positionX;
	@SerializedName("position.y")
	private double positionY;
	@SerializedName("position.alignment")
	private int positionAlignment;
	private double rotation;
	@SerializedName("scale.x")
	private double scaleX;
	@SerializedName("scale.y")
	private double scaleY;
	@SerializedName("scale.filter")
	private String scaleFilter;
	@SerializedName("crop.top")
	private int cropTop;
	@SerializedName("crop.right")
	private int cropRight;
	@SerializedName("crop.bottom")
	private int cropBottom;
	@SerializedName("crop.left")
	private int cropLeft;
	private boolean visible;
	private boolean locked;
	@SerializedName("bounds.type")
	private String boundsType;
	@SerializedName("bounds.alignment")
	private int boundsAlignment;
	@SerializedName("bounds.x")
	private double boundsX;
	@SerializedName("bounds.y")
	private double boundsY;
	private int sourceWidth;
	private int sourceHeight;
	private double width;
	private double height;
	private String parentGroupName;
	private List<SceneItemTransform> groupChildren;
	
	public double getPositionX() {
		return this.positionX;
	}
	public double getPositionY() {
		return this.positionY;
	}
	public int getPositionAlignment() {
		return this.positionAlignment;
	}
	public double getRotation() {
		return this.rotation;
	}
	public double getScaleX() {
		return this.scaleX;
	}
	public double getScaleY() {
		return this.scaleY;
	}
	public String getScaleFilter() {
		return this.scaleFilter;
	}
	public int getCropTop() {
		return this.cropTop;
	}
	public int getCropRight() {
		return this.cropRight;
	}
	public int getCropBottom() {
		return this.cropBottom;
	}
	public int getCropLeft() {
		return this.cropLeft;
	}
	public boolean isVisible() {
		return this.visible;
	}
	public boolean isLocked() {
		return this.locked;
	}
	public String getBoundsType() {
		return this.boundsType;
	}
	public int getBoundsAlignment() {
		return this.boundsAlignment;
	}
	public double getBoundsX() {
		return this.boundsX;
	}
	public double getBoundsY() {
		return this.boundsY;
	}
	public int getSourceWidth() {
		return this.sourceWidth;
	}
	public int getSourceHeight() {
		return this.sourceHeight;
	}
	public double getWidth() {
		return this.width;
	}
	public double getHeight() {
		return this.height;
	}
	public String getParentGroupName() {
		return this.parentGroupName;
	}
	public List<SceneItemTransform> getGroupChildren() {
		return this.groupChildren;
	}
}
