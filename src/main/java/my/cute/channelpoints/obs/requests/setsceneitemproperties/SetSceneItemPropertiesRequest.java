package my.cute.channelpoints.obs.requests.setsceneitemproperties;

import com.google.gson.annotations.SerializedName;

import my.cute.channelpoints.obs.requests.RequestBase;
import my.cute.channelpoints.obs.requests.RequestType;

public class SetSceneItemPropertiesRequest extends RequestBase {
	
	public static class Item {
		private final String name;
		private final Integer id;
		
		public Item(String name, Integer id) {
			this.name = name;
			this.id = id;
		}
		
		public Item(String name) {
			this.name = name;
			this.id = null;
		}
		
		public Item(int id) {
			this.name = null;
			this.id = id;
		}

		public String getName() {
			return this.name;
		}

		public Integer getId() {
			return this.id;
		}
	}

	@SerializedName("scene-name")
	private final String sceneName;
	private final Item item;
	@SerializedName("position.x")
	private final Double positionX;
	@SerializedName("position.y")
	private final Double positionY;
	@SerializedName("position.alignment")
	private final Integer positionAlignment;
	private final Double rotation;
	@SerializedName("scale.x")
	private final Double scaleX;
	@SerializedName("scale.y")
	private final Double scaleY;
	@SerializedName("scale.filter")
	private final String scaleFilter;
	@SerializedName("crop.top")
	private final Integer cropTop;
	@SerializedName("crop.bottom")
	private final Integer cropBottom;
	@SerializedName("crop.left")
	private final Integer cropLeft;
	@SerializedName("crop.right")
	private final Integer cropRight;
	private final Boolean visible;
	private final Boolean locked;
	@SerializedName("bounds.type")
	private final String boundsType;
	@SerializedName("bounds.alignment")
	private final Integer boundsAlignment;
	@SerializedName("bounds.x")
	private final Double boundsX;
	@SerializedName("bounds.y")
	private final Double boundsY;
	
	private SetSceneItemPropertiesRequest(SetSceneItemPropertiesRequest.Builder builder) {
		super(RequestType.SetSceneItemProperties);
		this.sceneName = builder.sceneName;
		this.item = new Item(builder.sceneItemName, builder.id);
		this.positionX = builder.positionX;
		this.positionY = builder.positionY;
		this.positionAlignment = builder.positionAlignment;
		this.rotation = builder.rotation;
		this.scaleX = builder.scaleX;
		this.scaleY = builder.scaleY;
		this.scaleFilter = builder.scaleFilter;
		this.cropTop = builder.cropTop;
		this.cropBottom = builder.cropBottom;
		this.cropLeft = builder.cropLeft;
		this.cropRight = builder.cropRight;
		this.visible = builder.visible;
		this.locked = builder.locked;
		this.boundsType = builder.boundsType;
		this.boundsAlignment = builder.boundsAlignment;
		this.boundsX = builder.boundsX;
		this.boundsY = builder.boundsY;
	}
	
	public String getSceneName() {
		return this.sceneName;
	}

	public Item getItem() {
		return this.item;
	}

	public Double getPositionX() {
		return this.positionX;
	}

	public Double getPositionY() {
		return this.positionY;
	}

	public Integer getPositionAlignment() {
		return this.positionAlignment;
	}

	public Double getRotation() {
		return this.rotation;
	}

	public Double getScaleX() {
		return this.scaleX;
	}

	public Double getScaleY() {
		return this.scaleY;
	}

	public String getScaleFilter() {
		return this.scaleFilter;
	}

	public Integer getCropTop() {
		return this.cropTop;
	}

	public Integer getCropBottom() {
		return this.cropBottom;
	}

	public Integer getCropLeft() {
		return this.cropLeft;
	}

	public Integer getCropRight() {
		return this.cropRight;
	}

	public Boolean getVisible() {
		return this.visible;
	}

	public Boolean getLocked() {
		return this.locked;
	}

	public String getBoundsType() {
		return this.boundsType;
	}

	public Integer getBoundsAlignment() {
		return this.boundsAlignment;
	}

	public Double getBoundsX() {
		return this.boundsX;
	}

	public Double getBoundsY() {
		return this.boundsY;
	}

	public static class Builder {
		private String sceneName;
		private String sceneItemName;
		private Integer id;
		private Double positionX;
		private Double positionY;
		private Integer positionAlignment;
		private Double rotation;
		private Double scaleX;
		private Double scaleY;
		private String scaleFilter;
		private Integer cropTop;
		private Integer cropBottom;
		private Integer cropLeft;
		private Integer cropRight;
		private Boolean visible;
		private Boolean locked;
		private String boundsType;
		private Integer boundsAlignment;
		private Double boundsX;
		private Double boundsY;
		
		public Builder sceneName(String sceneName) {
			this.sceneName = sceneName;
			return this;
		}
		
		public Builder sceneItemName(String name) {
			this.sceneItemName = name;
			return this;
		}
		
		public Builder sceneItemId(int id) {
			this.id = id;
			return this;
		}
		
		public Builder positionX(double position) {
			this.positionX = position;
			return this;
		}
		
		public Builder positionY(double position) {
			this.positionY = position;
			return this;
		}
		
		public Builder positionAlignment(int alignment) {
			this.positionAlignment = alignment;
			return this;
		}
		
		public Builder rotation(double rotation) {
			this.rotation = rotation;
			return this;
		}
		
		public Builder scaleX(double scale) {
			this.scaleX = scale;
			return this;
		}
		
		public Builder scaleY(double scale) {
			this.scaleY = scale;
			return this;
		}
		
		public Builder scaleFilter(String scaleFilter) {
			this.scaleFilter = scaleFilter;
			return this;
		}
		
		public Builder cropTop(int crop) {
			this.cropTop = crop;
			return this;
		}
		
		public Builder cropBottom(int crop) {
			this.cropBottom = crop;
			return this;
		}
		
		public Builder cropLeft(int crop) {
			this.cropLeft = crop;
			return this;
		}
		
		public Builder cropRight(int crop) {
			this.cropRight = crop;
			return this;
		}
		
		public Builder visible(boolean visible) {
			this.visible = visible;
			return this;
		}
		
		public Builder locked(boolean locked) {
			this.locked = locked;
			return this;
		}
		
		public Builder boundsType(String type) {
			this.boundsType = type;
			return this;
		}
		
		public Builder boundsAlignment(int alignment) {
			this.boundsAlignment = alignment;
			return this;
		}
		
		public Builder boundsX(double bounds) {
			this.boundsX = bounds;
			return this;
		}
		
		public Builder boundsY(double bounds) {
			this.boundsY = bounds;
			return this;
		}
		
		public SetSceneItemPropertiesRequest build() {
			if(this.sceneItemName == null && this.id == null) 
				throw new IllegalStateException("must provide a scene item name or id for this request");
			return new SetSceneItemPropertiesRequest(this);
		}
	}

}
