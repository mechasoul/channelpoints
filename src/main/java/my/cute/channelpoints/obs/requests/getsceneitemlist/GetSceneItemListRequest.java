package my.cute.channelpoints.obs.requests.getsceneitemlist;

import my.cute.channelpoints.obs.requests.RequestBase;
import my.cute.channelpoints.obs.requests.RequestType;

public class GetSceneItemListRequest extends RequestBase {

	private final String sceneName;
	
	public GetSceneItemListRequest() {
		super(RequestType.GetSceneItemList);
		this.sceneName = null;
	}

	public GetSceneItemListRequest(String sceneName) {
		super(RequestType.GetSceneItemList);
		this.sceneName = sceneName;
	}

	public String getSceneName() {
		return this.sceneName;
	}
}
