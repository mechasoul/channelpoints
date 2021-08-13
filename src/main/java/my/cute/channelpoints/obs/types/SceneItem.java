package my.cute.channelpoints.obs.types;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class SceneItem {

	//idk what cx, cy, x, y are
	private float cx, cy;
	private int alignment;
	private String name;
	private int id;
	private boolean render;
	private boolean muted;
	private boolean locked;
	@SerializedName("source_cx")
	private float sourceCx;
	@SerializedName("source_cy")
	private float sourceCy;
	private String type;
	private float volume;
	private float x, y;
	private String parentGroupName;
	private List<SceneItem> groupChildren;
	
	public float getCx() {
		return this.cx;
	}
	public float getCy() {
		return this.cy;
	}
	public int getAlignment() {
		return this.alignment;
	}
	public String getName() {
		return this.name;
	}
	public int getId() {
		return this.id;
	}
	public boolean isRender() {
		return this.render;
	}
	public boolean isMuted() {
		return this.muted;
	}
	public boolean isLocked() {
		return this.locked;
	}
	public float getSourceCx() {
		return this.sourceCx;
	}
	public float getSourceCy() {
		return this.sourceCy;
	}
	public String getType() {
		return this.type;
	}
	public float getVolume() {
		return this.volume;
	}
	public float getX() {
		return this.x;
	}
	public float getY() {
		return this.y;
	}
	public String getParentGroupName() {
		return this.parentGroupName;
	}
	public List<SceneItem> getGroupChildren() {
		return this.groupChildren;
	}
	
}
