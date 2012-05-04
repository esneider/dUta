package ar.edu.itba.pdc.duta.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import net.jcip.annotations.ThreadSafe;

import org.apache.log4j.Logger;

import proxy.RequestChannelHandler;

@ThreadSafe
public class Server {
	
	private static Server instance;
	
	private ReactorPool reactorPool;
	
	private Server() {
		super();
	}
	
	public void start() {
		
		Logger logger = Logger.getLogger(Server.class);
		try {
			runReactors();
		} catch (IOException e) {
			logger.fatal("Failed to start Reactors", e);
			return;
		}
		
		try {
			int port = 9999;
			
			Selector selector = Selector.open();
			ServerSocketChannel serverChannel = selector.provider().openServerSocketChannel();
			
			serverChannel.configureBlocking(false);
			serverChannel.socket().bind(new InetSocketAddress(port));
			
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);
			
			logger.info("Starting server on port: " + port);
			
			while (true) {
				if (selector.select() > 0) {
					Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
					while (keys.hasNext()) {
						SelectionKey key = keys.next();
						keys.remove();

						ServerSocketChannel channel = (ServerSocketChannel) key.channel();
						SocketChannel socket = channel.accept();
						if (socket != null) {
							ChannelHandler handler = new RequestChannelHandler();
							getReactor().addChannel(socket, handler);
						}
					}
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		reactorPool.close();
		reactorPool = null;
	}
	
	private void runReactors() throws IOException {
		int threads = 2 * Runtime.getRuntime().availableProcessors();
		reactorPool = new ReactorPool(threads);
		reactorPool.start();
	}

	private Reactor getReactor() {
		return reactorPool.get();
	}
	
	public static void main(String[] args) throws IOException {
		Server.run();
		System.exit(0);
	}

	private static void run() {
		instance = new Server();
		instance.start();
	}

	public static void registerChannel(SocketChannel channel, ChannelHandler handler) throws IOException {
		instance.getReactor().addChannel(channel, handler);
	}
}
