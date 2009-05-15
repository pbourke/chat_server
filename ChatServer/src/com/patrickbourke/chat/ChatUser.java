package com.patrickbourke.chat;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Implements the Chat server application protocol.
 * Not thread safe - assumes that we are running in single-threaded model
 * 
 * @author Patrick Bourke <pb@patrickbourke.com>
 */
public class ChatUser {
    final static Map<String, List<ChatUser>> ROOM_MAP = new HashMap<String, List<ChatUser>>();
    
    // set after the Login operation
    protected String username;
    
    // messages queued for user
    protected Queue<ByteBuffer> outboundMsgQueue = new LinkedBlockingQueue<ByteBuffer>(25);
    
    // can this be made the attachment object?
        // yes - room state is shared in ROOM_MAP object
    
    public void onAccept(final SelectionKey key) {
        // create an object to track connection state
        // attach object to key
        // next step: READ
    }
    
    public void onRead(final SelectionKey key, final ByteBuffer input) {
        // was input valid based on connection state?
        // if input not valid based on connection state
            // register error message
            // flag connection for closing
            // next step: WRITE
        
        // handle message by type:
            // case login:
            // case join:
            // case part:
            // case message to room:
            // case message to individual:
    }
    
    public void onWrite(final SelectionKey key) {
        
    }
    
    public void onClose(final SelectionKey key) {
        // remove user from any registered rooms
        // close connection, etc.
    }
}
