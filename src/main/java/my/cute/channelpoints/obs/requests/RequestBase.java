package my.cute.channelpoints.obs.requests;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.annotations.SerializedName;

public abstract class RequestBase {

	protected static final transient Logger log = LoggerFactory.getLogger(RequestBase.class);
	private static final transient AtomicLong idCounter = new AtomicLong(0);
	
	@SerializedName("request-type")
	private RequestType requestType;
	
	@SerializedName("message-id")
	private String messageId;
	
	public RequestBase(RequestType requestType) {
		this.requestType = requestType;
		this.messageId = String.valueOf(idCounter.incrementAndGet());
	}
	
	public RequestType getRequestType() {
		return this.requestType;
	}
	
	public String getMessageId() {
		return this.messageId;
	}
	
}
