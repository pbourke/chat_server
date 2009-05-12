package com.patrickbourke.chat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main class for chat server program
 * 
 * @author bourke
 */
public class Server {
	// room
	// clients in the room
	// message to the room
	// Users
	// private message
	
	// echo server.
	// Usage: Run this server and telnet to localhost port 1234
	public static void main(String[] args) {
		final Logger log = Logger.getLogger("chat");
		try {
			log.info("Starting Server");
			final ServerSocketChannel serverChannel = ServerSocketChannel.open();
			serverChannel.configureBlocking(false);
			serverChannel.socket().bind( new InetSocketAddress(1234) );
			
			final Selector selector = Selector.open();
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);
			
			while (true) {
				int numReady = selector.select();
				if ( numReady <= 0 ) {
					continue;
				}
				
				for ( Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
					  iter.hasNext(); ) {
					SelectionKey key = iter.next();
					iter.remove();
					
					if ( !key.isValid() ) {
						log.info("Key " + key + " is not valid");
						continue;
					}
					
					// handle new client connection
					if ( key.isAcceptable() ) {
						log.info("Accepting a new client connection");
						final SocketChannel socketChannel = ((ServerSocketChannel)key.channel()).accept();
						socketChannel.configureBlocking(false);
						socketChannel.register(key.selector(), SelectionKey.OP_READ);
					}
					
					// handle reading client message and connection close
					if ( key.isReadable() ) {
						final ByteBuffer msgBuf = ByteBuffer.allocate(1024);
						final int numBytesRead = ((SocketChannel)key.channel()).read(msgBuf);
						if ( numBytesRead == -1 ) {
							// client closed connection
							log.info("Client closed the connection");
							key.channel().close();
							key.cancel();
							continue;
						}

						// read the message
						msgBuf.flip();
						
						// create a return message and register interest in a write event
						final ByteBuffer returnBuf = ByteBuffer.allocate(1040);
						returnBuf.put("YOU SAID: ".getBytes(), 0, "YOU SAID: ".length());
						returnBuf.put(msgBuf.array(), 0, msgBuf.limit());
						returnBuf.flip();
						key.interestOps(SelectionKey.OP_WRITE);
						key.attach(returnBuf);
						msgBuf.clear();
					}
					
					if ( key.isWritable() ) {
						log.info("Writing echo message to the client");
						// write the attachment to the channel						
						ByteBuffer msg = (ByteBuffer) key.attachment();
						int bytesWritten = ((SocketChannel)key.channel()).write(msg);
						log.info("Wrote " + bytesWritten + " bytes to the client");

						// if any data remains, keep in write mode
						// otherwise, flip to read mode
						if ( !msg.hasRemaining() ) {
							key.attach(null);
							key.interestOps(SelectionKey.OP_READ);
						}
					}
				}
			}
		
		} catch (IOException e) {
			log.log(Level.SEVERE, "IOException occurred", e);
		}
	}
}
