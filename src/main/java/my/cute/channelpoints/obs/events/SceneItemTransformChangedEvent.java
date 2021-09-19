package my.cute.channelpoints.obs.events;

import com.google.gson.annotations.SerializedName;

import my.cute.channelpoints.obs.types.SceneItemTransform;

public class SceneItemTransformChangedEvent extends EventBase {

	@SerializedName("scene-name")
	private String sceneName;
	@SerializedName("item-name")
	private String itemName;
	@SerializedName("item-id")
	private int itemId;
	private SceneItemTransform transform;
	
	public SceneItemTransformChangedEvent() {
		super(EventType.SceneItemTransformChanged);
	}
	
	public String getSceneName() {
		return this.sceneName;
	}

	public String getItemName() {
		return this.itemName;
	}

	public int getItemId() {
		return this.itemId;
	}

	public SceneItemTransform getTransform() {
		return this.transform;
	}

}
