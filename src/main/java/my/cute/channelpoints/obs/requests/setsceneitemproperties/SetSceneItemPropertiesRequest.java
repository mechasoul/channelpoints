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
	
	public static class Position {
		private final Double x;
		private final Double y;
		private final Integer alignment;
		
		public Position(Double x, Double y, Integer alignment) {
			this.x = x;
			this.y = y;
			this.alignment = alignment;
		}
		
		public Double getX() {
			return this.x;
		}
		
		public Double getY() {
			return this.y;
		}
		
		public Integer getAlignment() {
			return this.alignment;
		}
	}
	
	public static class Scale {
		private final Double x;
		private final Double y;
		private final String filter;
		
		public Scale(Double x, Double y, String filter) {
			this.x = x;
			this.y = y;
			this.filter = filter;
		}
		
		public Double getX() {
			return this.x;
		}
		
		public Double getY() {
			return this.y;
		}
		
		public String getFilter() {
			return this.filter;
		}
	}
	
	public static class Crop {
		private final Integer top;
		private final Integer bottom;
		private final Integer left;
		private final Integer right;
		
		public Crop(Integer top, Integer bottom, Integer left, Integer right) {
			this.top = top;
			this.bottom = bottom;
			this.left = left;
			this.right = right;
		}
		
		public Integer getTop() {
			return this.top;
		}
		
		public Integer getBottom() {
			return this.bottom;
		}
		
		public Integer getLeft() {
			return this.left;
		}
		
		public Integer getRight() {
			return this.right;
		}
	}
	
	public static class Bounds {
		private final String type;
		private final Integer alignment;
		private final Double x;
		private final Double y;
		
		public Bounds(String type, Integer alignment, Double x, Double y) {
			this.type = type;
			this.alignment = alignment;
			this.x = x;
			this.y = y;
		}
		
		public String getType() {
			return this.type;
		}
		
		public Integer getAlignment() {
			return this.alignment;
		}
		
		public Double getX() {
			return this.x;
		}
		
		public Double getY() {
			return this.y;
		}
	}

	/*
	 * implementation note
	 * all primitives in this class are boxed because this is a general request for
	 * setting any of a number of scene item properties. as such, every field except
	 * item is optional (and even item only requires a name or id), and by boxing 
	 * every primitive we can take advantage of gson skipping nulls during 
	 * serialization
	 */
	@SerializedName("scene-name")
	private final String sceneName;
	private final Item item;
	private final Position position;
	private final Double rotation;
	private final Scale scale;
	private final Crop crop;
	private final Boolean visible;
	private final Boolean locked;
	private final Bounds bounds;
	
	private SetSceneItemPropertiesRequest(SetSceneItemPropertiesRequest.Builder builder) {
		super(RequestType.SetSceneItemProperties);
		this.sceneName = builder.sceneName;
		this.item = new Item(builder.sceneItemName, builder.id);
		
		if(builder.positionX != null || builder.positionY != null || builder.positionAlignment != null)
			this.position = new Position(builder.positionX, builder.positionY, builder.positionAlignment);
		else
			this.position = null;
		
		this.rotation = builder.rotation;
		
		if(builder.scaleX != null || builder.scaleY != null || builder.scaleFilter != null) 
			this.scale = new Scale(builder.scaleX, builder.scaleY, builder.scaleFilter);
		else
			this.scale = null;
		
		if(builder.cropTop != null || builder.cropBottom != null || builder.cropLeft != null || builder.cropRight != null)
			this.crop = new Crop(builder.cropTop, builder.cropBottom, builder.cropLeft, builder.cropRight);
		else
			this.crop = null;
		
		this.visible = builder.visible;
		this.locked = builder.locked;
		
		if(builder.boundsType != null || builder.boundsAlignment != null || builder.boundsX != null || builder.boundsY != null)
			this.bounds = new Bounds(builder.boundsType, builder.boundsAlignment, builder.boundsX, builder.boundsY);
		else
			this.bounds = null;
	}
	
	public String getSceneName() {
		return this.sceneName;
	}

	public Item getItem() {
		return this.item;
	}

	public Position getPosition() {
		return this.position;
	}

	public Double getRotation() {
		return this.rotation;
	}

	public Scale getScale() {
		return this.scale;
	}

	public Crop getCrop() {
		return this.crop;
	}

	public Boolean getVisible() {
		return this.visible;
	}

	public Boolean getLocked() {
		return this.locked;
	}

	public Bounds getBounds() {
		return this.bounds;
	}

	/**
	 * not safe for use by more than one thread
	 */
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
