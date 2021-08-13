package my.cute.channelpoints.obs.requests.deletesceneitem;

import com.google.gson.annotations.SerializedName;

import my.cute.channelpoints.obs.requests.RequestBase;
import my.cute.channelpoints.obs.requests.RequestType;

public class DeleteSceneItemRequest extends RequestBase {

	public static class Item {
		
		private final String name;
		private final int id;
		
		private Item(String name, int id) {
			this.name = name;
			this.id = id;
		}
		
		private Item(int id) {
			this.name = null;
			this.id = id;
		}

		public String getName() {
			return this.name;
		}

		public int getId() {
			return this.id;
		}
	}
	
	@SerializedName("scene")
	private final String sceneName;
	private final Item item;
	
	public DeleteSceneItemRequest(String sceneName, String itemName, int itemId) {
		super(RequestType.DeleteSceneItem);
		this.sceneName = sceneName;
		this.item = new Item(itemName, itemId);
	}

	public String getSceneName() {
		return this.sceneName;
	}

	public Item getItem() {
		return this.item;
	}
}
