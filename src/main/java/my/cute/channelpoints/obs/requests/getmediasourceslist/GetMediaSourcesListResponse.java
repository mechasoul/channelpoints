package my.cute.channelpoints.obs.requests.getmediasourceslist;

import java.util.List;

import my.cute.channelpoints.obs.requests.ResponseBase;

public class GetMediaSourcesListResponse extends ResponseBase {

	public static class MediaSource {
		
		private String sourceName;
		private String sourceKind;
		private String mediaState;
		
		public String getSourceName() {
			return this.sourceName;
		}
		public String getSourceKind() {
			return this.sourceKind;
		}
		public String getMediaState() {
			return this.mediaState;
		}
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("MediaSource [sourceName=");
			builder.append(sourceName);
			builder.append(", sourceKind=");
			builder.append(sourceKind);
			builder.append(", mediaState=");
			builder.append(mediaState);
			builder.append("]");
			return builder.toString();
		}
	}
	
	private List<MediaSource> mediaSources;

	public List<MediaSource> getMediaSources() {
		return this.mediaSources;
	}
}
