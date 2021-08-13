package my.cute.channelpoints.obs.requests.getcurrentscene;

import java.util.List;

import my.cute.channelpoints.obs.requests.ResponseBase;
import my.cute.channelpoints.obs.types.SceneItem;

public class GetCurrentSceneResponse extends ResponseBase {

	private String name;
	private List<SceneItem> sources;
	
	public String getName() {
		return this.name;
	}
	public List<SceneItem> getSources() {
		return this.sources;
	}
	
}
