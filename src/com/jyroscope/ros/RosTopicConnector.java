package com.jyroscope.ros;

import com.jyroscope.Link;
import com.jyroscope.ros.types.RosMessageType;
import com.jyroscope.ros.tcpros.TCPROSRemoteToLocalConnection;
import java.io.*;
import java.net.*;
import com.jyroscope.*;
import com.jyroscope.ros.types.*;
import com.jyroscope.server.xmlrpc.*;
import com.jyroscope.types.*;
import com.jyroscope.util.*;

public class RosTopicConnector {
    
    private final RosTopic topic;
    private final URI slaveURI;
    private final RosSlave localSlave;
    private Link<RosMessage> listener;
    private volatile boolean connected = false;
    
    public RosTopicConnector(RosTopic topic, URI slaveURI, RosSlave localSlave) {
        this.topic = topic;
        this.slaveURI = slaveURI;
        this.localSlave = localSlave;
    }
    
    /*
     * Opens a persistent TCPROS connection to the remote Slave.
     * Throws an exception if the connection fails (meaning that the listener is NOT connected and therefore the internal list of listeners is set to be empty).
     */
    public void connect(Link<RosMessage> listener) {
        this.listener = listener;
        connected = true;
        Do.later(new Runnable() {
            @Override
            public void run() {
                try {
                    open();
                } catch (Exception e) {
                    // TODO handle errors better
                    e.printStackTrace();
                }
            }
        });
    }
    
    private void open() throws SystemException {
        try {
            XMLRPCClient slave = new XMLRPCClient(slaveURI);
            XMLRPCArray result = (XMLRPCArray)slave.call("requestTopic", new XMLRPCArray(new Object[] {localSlave.getCallerId(), topic.getName().toNameString(), new XMLRPCArray(new Object[] {new XMLRPCArray(new Object[] {"TCPROS"})})}));
    
            if (result.get(0).equals(1)) {
                XMLRPCArray parameters = (XMLRPCArray)result.get(2);
                String host = (String)parameters.get(1);
                Integer port = (Integer)parameters.get(2);
                RosMessageType rosType = (RosMessageType)RosTypes.getType(topic.getRosType());
                
                final TCPROSRemoteToLocalConnection subscriber = new TCPROSRemoteToLocalConnection(host, port);
                subscriber.setParameters(localSlave.getCallerId(), topic.getName().toNameString(), rosType);
                
                if (!subscriber.connect())
                    throw new SystemException("Unexpected end of stream while connecting to publisher");
                
                try {
                    RosMessage buffer = new RosMessage(rosType);
                    while (connected) {
                        if (subscriber.read(buffer))
                            listener.handle(buffer);
                        else {
                            Log.msg(this, "Publisher closed connection");
                            break;
                        }
                    }
                    
                    try {
                        subscriber.close();
                    } catch (IOException ioe) {
                        Log.exception(RosTopicConnector.this, ioe, "Error while closing connection to topic " + topic);
                    }
                } catch (FormatException fe) {
                    Log.exception(RosTopicConnector.this, fe, "Format exception while reading from connection to topic " + topic);
                    try {
                        subscriber.close();
                    } catch (IOException ioe) {
                        Log.exception(RosTopicConnector.this, ioe, "Error while closing connection to topic " + topic);
                    }
                } catch (IOException e) {
                    Log.exception(RosTopicConnector.this, e, "Exception while reading from connection to topic " + topic);
                }
            } else {
                throw new SystemException("Could not open TCPROS connection to " + slaveURI + " (" + String.valueOf(result.get(1)) + ")");
            }
        } catch (XMLRPCException | IOException e) {
            throw new SystemException("Could not open TCPROS connection to " + slaveURI, e);
        } catch (ConversionException ex) {
            throw new SystemException("Could not create TCPROS connection", ex);
        }        
    }

    void disconnect() {
        // TODO handle this better -- try to unblock the subscriber.read() so that it isn't sitting around on an unsubscribed connection
        // Note that this method should not block
        connected = false;
    }

    
}
