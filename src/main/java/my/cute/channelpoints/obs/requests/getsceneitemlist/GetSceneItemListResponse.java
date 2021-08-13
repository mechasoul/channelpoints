package my.cute.channelpoints.obs.requests.getsceneitemlist;

import java.util.List;

import my.cute.channelpoints.obs.requests.ResponseBase;
import my.cute.channelpoints.obs.types.SimpleSceneItem;

public class GetSceneItemListResponse extends ResponseBase {

	private String sceneName;
	private List<SimpleSceneItem> sceneItems;
	
	public String getSceneName() {
		return this.sceneName;
	}
	public List<SimpleSceneItem> getSceneItems() {
		return this.sceneItems;
	}
	
}
