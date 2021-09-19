package my.cute.channelpoints.obs.requests.getvideoinfo;

import my.cute.channelpoints.obs.requests.ResponseBase;

public class GetVideoInfoResponse extends ResponseBase {

	private int baseWidth;
	private int baseHeight;
	
	private int outputWidth;
	private int outputHeight;
	
	private String scaleType;
	
	private double fps;
	
	private String videoFormat;
	private String colorSpace;
	private String colorRange;
	
	public int getBaseWidth() {
		return this.baseWidth;
	}
	public int getBaseHeight() {
		return this.baseHeight;
	}
	public int getOutputWidth() {
		return this.outputWidth;
	}
	public int getOutputHeight() {
		return this.outputHeight;
	}
	public String getScaleType() {
		return this.scaleType;
	}
	public double getFps() {
		return this.fps;
	}
	public String getVideoFormat() {
		return this.videoFormat;
	}
	public String getColorSpace() {
		return this.colorSpace;
	}
	public String getColorRange() {
		return this.colorRange;
	}
}
