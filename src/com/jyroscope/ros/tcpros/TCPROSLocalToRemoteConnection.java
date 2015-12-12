package com.jyroscope.ros.tcpros;

import com.jyroscope.Link;
import com.jyroscope.ros.RosTopic;
import com.jyroscope.ros.types.RosTypes;
import com.jyroscope.ros.types.RosMessageType;
import java.io.*;
import java.net.*;
import com.jyroscope.*;
import com.jyroscope.ros.*;
import com.jyroscope.types.*;
import java.util.*;

public class TCPROSLocalToRemoteConnection implements Link<RosMessage> {

    private TCPROSServer server;
    private Socket socket;
    private InputStream is;
    private OutputStream os;
    
    private RosTopic topic;
    
    private boolean closed;
    private ArrayDeque<RosMessage> messages;
    
    public TCPROSLocalToRemoteConnection(TCPROSServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
        this.messages = new ArrayDeque<>();
    }
    
    public void open() {
        try {
            is = socket.getInputStream();
            os = new BufferedOutputStream(socket.getOutputStream());
            
            TCPROSHeader header = new TCPROSHeader();
            RosMessage buffer = new RosMessage();
            if (buffer.readIn(is)) {
                header.parse(buffer);
            
                // For some reason, the rostopic echo client reconnects with empty headers during a disconnect
                if (header.count() != 0) {

                    String caller = header.getHeader("callerid");
                    String topicName = header.getHeader("topic");
                    String md5sum = header.getHeader("md5sum");
                    String typeName = header.getHeader("type");

                    final RosMessageType type = RosTypes.getMessageType(typeName);

                    String error;
                    if (type == null)
                        error = "Unrecognized type name " + typeName;
                    else if (!type.getHash().equals(md5sum))
                        error = "MD5 checksums do not match for type " + typeName;
                    else {
                        topic = server.findTopic(caller, topicName);

                        if (topic == null)
                            error = "Unrecognized topic " + topicName;
                        else {

                            TCPROSHeader reply = new TCPROSHeader();
                            reply.putHeader("md5sum", type.getHash());
                            reply.putHeader("type", typeName);
                            reply.render(buffer);
                            buffer.writeOut(os);
                            os.flush();
                            try {
                                topic.addRemoteSubscriber(this);
                            } catch (ConversionException ce) {
                                Log.exception(this, ce, "Cannot convert ROS message to subscriber types");
                                throw new RuntimeException(ce);
                            }
                            // Successful connection established!
                            // Process messages
                            messageLoop();
                            return;
                        }
                    }

                    // at this point, error != null
                    // TODO verify this -- the specification/documentation isn't very clear about what to do in the event of an error
                    TCPROSHeader reply = new TCPROSHeader();
                    reply.putHeader("error", error);
                    reply.render(buffer);
                    buffer.writeOut(os);
                    Log.msg(this, error);
                    socket.close();

                } else {
                    // The client has sent empty headers. Just disconnect.
                    socket.close();
                }
            } else {
                // The client has not sent full headers. Just disconnect.
                socket.close();
            }
        } catch (SystemException se) {
            Log.exception(this, se, "Could not connect to publisher while communicating with TCPROS client");
        } catch (IOException e) {
            Log.warn(this, "Error while communicating with TCPROS client, disconnecting");
            topic.removeRemoteSubscriber(this);
            try {
                socket.close();
            } catch (IOException ioe2) {
                Log.exception(this, ioe2, "Error while closing socket");
            }
        }
    }
    
    private void messageLoop() {
        try {
            while (true) {
                RosMessage message;
                boolean hasNext;
                synchronized (this) {
                    message = messages.poll();
                    hasNext = messages.isEmpty();
                    if (message == null && !closed) {
                        try {
                            this.wait();
                        } catch (InterruptedException ex) {
                            // do nothing
                        }
                    }
                }
                if (message != null) {
                    message.writeOut(os);
                    if (hasNext)
                        os.flush();
                } else if (closed)
                    break;
            }
        } catch (IOException ioe) {
            Log.exception(this, ioe, "Error while communicating with TCPROS client");
        }
        
        topic.removeRemoteSubscriber(this);
        
        try {
            socket.close();
        } catch (IOException ioe) {
            Log.exception(this, ioe, "Error while closing socket");
        }
    }

    @Override
    public Class<? extends RosMessage> getType() {
        return RosMessage.class;
    }

    @Override
    public synchronized void handle(RosMessage message) {
        messages.offer(message);
        this.notify();
    }

    public synchronized void close() {
        // The queue is drained before the close takes effect
        closed = true;
        this.notify();
    }

}
