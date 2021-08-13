package my.cute.channelpoints.obs.events;

import com.google.gson.annotations.SerializedName;

public class SceneItemVisibilityChangedEvent extends EventBase {

	@SerializedName("scene-name")
	private String sceneName;
	@SerializedName("item-name")
	private String itemName;
	@SerializedName("item-id")
	private int itemId;
	@SerializedName("item-visible")
	private boolean itemVisible;
	
	public SceneItemVisibilityChangedEvent() {
		super(EventType.SceneItemVisibilityChanged);
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

	public boolean isItemVisible() {
		return this.itemVisible;
	}
}
