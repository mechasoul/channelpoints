package my.cute.channelpoints.obs.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.annotations.SerializedName;

public abstract class ResponseBase {

	protected static final transient Logger log = LoggerFactory.getLogger(RequestBase.class);
	
	@SerializedName("message-id")
	private String messageId;
	private String status;
	private String error;
	
	public String getMessageId() {
		return this.messageId;
	}
	
	public String getStatus() {
		return this.status;
	}
	
	public String getError() {
		return this.error;
	}
	
}
