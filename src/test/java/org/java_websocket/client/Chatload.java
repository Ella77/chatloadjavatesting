package org.java_websocket.client;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class Chatload extends WebSocketClient {

	static int received = 0;
	static int send = 0;
	static long sum = 0;

	public Chatload(URI serverUri, Draft draft) {
		super(serverUri, draft);
	}

	public Chatload(URI serverURI) {
		super(serverURI);
	}

	public Chatload(URI serverUri, Map<String, String> httpHeaders) {
		super(serverUri, httpHeaders);
	}

	@Override
	public void onOpen(ServerHandshake handshakedata) {
		 // send("hello world");
		System.out.println("connected");
	}

	@Override
	public void onMessage(String message) {
		long dt = (System.currentTimeMillis() - Long.valueOf(message));
		System.out.println("received: " + dt + " ms");
		received++;
		sum += dt;

		/*
		try {
			long dt = (System.currentTimeMillis() - Long.valueOf(message.split(" ")[1]));
			System.out.println("received: " + dt + " ms");
			received++;
			sum += dt;
		} catch (Exception e) {
			// do nothing
		}*/
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		 System.out.println(
		 "Connection closed by " + (remote ? "remote peer" : "us") + " Code: " + code
		 + " Reason: " + reason);
	}

	@Override
	public void onError(Exception ex) {
		ex.printStackTrace();
	}

	private static boolean allConnected(List<Chatload> connections) {
		int count = 0;
		for (int i = 0; i < connections.size(); i++) {
			if (connections.get(i).isOpen()) {
				count++;
			}
		}
		if (count == connections.size())
			return true;
		else
			return false;
	}

	static class Sending implements Runnable {
		private Chatload e;
		public Sending(Chatload example) {
			this.e = example;
		}

		@Override
		public void run() {
			e.send(String.valueOf(System.currentTimeMillis()));
		}
		
	}

	public static void main(String[] args) throws Exception {
		int clients = 1000;
		int pub = 10;
		List<Socket> connections = new ArrayList<Socket>();

		for(int i = 0; i < clients; i++) {
			final Socket sc = IO.socket("http://52.79.243.195:3000");
			sc.connect();
			sc.on(sc.EVENT_CONNECT, new Emitter.Listener() {
	
				  @Override
				  public void call(Object... args) {
				    sc.emit("add user", sc.id());
				  }
	
				}).on(Socket.EVENT_CONNECT_TIMEOUT, new Emitter.Listener() {
	                @Override
	                public void call(Object... args) {
	                	sc.disconnect();
	                	sc.connect();
	                }
	            }).on("new message", new Emitter.Listener() {
					
					  @Override
					  public void call(Object... args) {
						  JSONObject obj = (JSONObject)args[0];
						  try {
							  received++;
							  long c = System.currentTimeMillis() - Long.valueOf(obj.get("message").toString());
							  sum += c;
							System.out.println("latency: " + c + " at " + sc.id());
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					  }
		
					});

			connections.add(sc);
			while(!sc.connected()) {
				Thread.sleep(70);
			}
			System.out.println(i + " has connected");

		}
		
		int conn = 0;
		while(conn < clients) {
			conn = 0;
			for(int i = 0; i < clients; i++) {
				if(connections.get(i).connected()) {
					conn++;
				}
					
			}
			System.out.println(conn);
			Thread.sleep(1000);
		}
		
		for (int i = 0; i < pub; i++) {
			Socket sc = connections.get(i);
			sc.emit("new message", String.valueOf(System.currentTimeMillis()));
		}
		
		while(true) {
			if(received > 0 && sum > 0)
				System.out.println("total: " + received + " sum: " + (sum / received));
		  Thread.sleep(2000);
		}
	}

/*
	public static void main(String[] args) throws Exception {

		List<Example> connections = new ArrayList<Example>();

		for (int i = 0; i < rooms; i++) {
			for (int j = 0; j < clients; j++) {
				String uri = ("ws://13.112.50.10:8080/chat");
				Example c = new Example(new URI(uri));
				c.connect();
				connections.add(c);
				Thread.sleep(50);
			}
		}
		while (!allConnected(connections)) {
			System.out.print(".");
			Thread.sleep(2000);
		}
		
		int pub = 100;
		
		// messsaging benchmark
		for (int i = 0; i < pub; i++) {
			Sending r = new Sending(connections.get(i));
			r.run();
			send++;
		}
		
		while(true) {
			if(sum > 0 && received > 0) {
				System.out.println(clients + " sub, " + pub + " pub" + ", received msg: " + received);
			  System.out.println((sum/received));
			}
			Thread.sleep(1000);
		}
	}
*/
}