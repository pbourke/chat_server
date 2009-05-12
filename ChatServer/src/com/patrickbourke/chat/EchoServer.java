package com.patrickbourke.chat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main class for echo server test program.
 * 
 * Usage: run the EchoServer using JVM 1.5/1.6.
 * 
 * To connect a client to the server telnet to the server port. ie:
 *      telnet localhost 1234
 * 
 * Type text followed by enter to have it echoed back.
 * 
 * @author Patrick Bourke <pb@patrickbourke.com>
 */
public class EchoServer {
	public static void main(String[] args) {
		final Logger log = Logger.getLogger("chat");
		try {
		    // open the Server socket, bind it to an address 
		    // and configure it for non-blocking operation
			log.info("Starting Server");
			final ServerSocketChannel serverChannel = ServerSocketChannel.open();
			serverChannel.configureBlocking(false);
			serverChannel.socket().bind( new InetSocketAddress(1234) );
			
			final Selector selector = Selector.open();
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);

			final ByteBuffer inputBuf = ByteBuffer.allocate(1024);
			
			// main event loop - accept new connections and handle read and write
			// operations
			while (true) {
			    // are there any pending events?
				int numReady = selector.select();
				if ( numReady <= 0 ) {
					continue;
				}
				
				// process each event (ready SelectionKey) in sequence
				for ( Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
					  iter.hasNext(); ) {
				    
				    // obtain the key and ensure that it's valid before doing anything
				    // with it
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
                        // read the message
						final int numBytesRead = ((SocketChannel)key.channel()).read(inputBuf);
						if ( numBytesRead == -1 ) {
							// client closed connection
							log.info("Client closed the connection");
							
							// remove SelectionKey from the Selector so that we receive no more events
							// from it
							key.channel().close();
							key.cancel();
	                        inputBuf.clear();
							continue;
						}

						log.info("Read " + numBytesRead + " bytes from the client");
						// flip() makes the buffer ready for reading - sets position to 0 and 
						// limit to the previous position
						inputBuf.flip();
						
						// create a return message and register interest in a write event, which
						// will be handled in a subsequent event
						final ByteBuffer outputBuf = ByteBuffer.allocate(1040);
						outputBuf.put("YOU SAID: ".getBytes(), 0, "YOU SAID: ".length());
						outputBuf.put(inputBuf.array(), 0, inputBuf.limit());
						outputBuf.flip();
						key.interestOps(SelectionKey.OP_WRITE);
						
						// associate the return message buffer with the key, so that the write event
						// can write it to the appropriate channel						
						key.attach(outputBuf);
						inputBuf.clear();
					}
					
					// handle echo back to the client
					if ( key.isWritable() ) {
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
