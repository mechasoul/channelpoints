package my.cute.channelpoints.obs.requests.getstudiomodestatus;

import com.google.gson.annotations.SerializedName;

import my.cute.channelpoints.obs.requests.ResponseBase;

public class GetStudioModeStatusResponse extends ResponseBase {

	@SerializedName("studio-mode")
	private boolean studioMode;

	public boolean isStudioMode() {
		return this.studioMode;
	}
}
