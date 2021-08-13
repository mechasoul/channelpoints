package my.cute.channelpoints.obs.events;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import my.cute.channelpoints.obs.types.SceneItem;

public class SwitchScenesEvent extends EventBase {

	@SerializedName("scene-name")
	private String sceneName;
	private List<SceneItem> sources;
	
	public SwitchScenesEvent() {
		super(EventType.SwitchScenes);
	}
	
	public String getSceneName() {
		return this.sceneName;
	}
	public List<SceneItem> getSources() {
		return this.sources;
	}
}
