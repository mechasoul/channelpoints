package my.cute.channelpoints.obs.requests.getsceneitemlist;

import my.cute.channelpoints.obs.requests.RequestBase;
import my.cute.channelpoints.obs.requests.RequestType;

public class GetSceneItemListRequest extends RequestBase {

	private String sceneName = null;
	
	public GetSceneItemListRequest() {
		super(RequestType.GetSceneItemList);
	}

	public GetSceneItemListRequest(String sceneName) {
		super(RequestType.GetSceneItemList);
		this.sceneName = sceneName;
	}

	public String getSceneName() {
		return this.sceneName;
	}
}
