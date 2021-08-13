package my.cute.channelpoints.obs.types;

/**
 * used in some request responses, eg GetSceneItemListRequest
 */
public class SimpleSceneItem {
	
	private int itemId;
	private String sourceKind;
	private String sourceName;
	private String sourceType;

	public int getItemId() {
		return this.itemId;
	}

	public String getSourceKind() {
		return this.sourceKind;
	}

	public String getSourceName() {
		return this.sourceName;
	}

	public String getSourceType() {
		return this.sourceType;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SimpleSceneItem [itemId=");
		builder.append(itemId);
		builder.append(", sourceKind=");
		builder.append(sourceKind);
		builder.append(", sourceName=");
		builder.append(sourceName);
		builder.append(", sourceType=");
		builder.append(sourceType);
		builder.append("]");
		return builder.toString();
	}
}