package my.cute.channelpoints.obs;

import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

public class MyListener implements WebSocket.Listener {
	
	private final MessageReceiver messageReceiver;
	private final ExecutorService executor;
	
	MyListener(MessageReceiver receiver, ExecutorService executor) {
		this.messageReceiver = receiver;
		this.executor = executor;
	}

	@Override
	public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
		webSocket.request(1);
		this.executor.submit(() -> {
			try {
				this.messageReceiver.receiveMessage(data.toString());
			} catch (Throwable th) {
				th.printStackTrace();
			}
		});
		return null;
	}

	@Override
	public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
		return Listener.super.onBinary(webSocket, data, last);
	}

	@Override
	public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
		return Listener.super.onPing(webSocket, message);
	}

	@Override
	public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
		return Listener.super.onPong(webSocket, message);
	}

	@Override
	public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
		return Listener.super.onClose(webSocket, statusCode, reason);
	}

	@Override
	public void onError(WebSocket webSocket, Throwable error) {
		System.out.println("websocket error: " + error.toString());
		error.printStackTrace();
		Listener.super.onError(webSocket, error);
	}

	@Override
	public void onOpen(WebSocket socket) {
		System.out.println("open: " + socket);
		Listener.super.onOpen(socket);
	}
	
}
