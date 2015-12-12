package com.jyroscope.ros;

import com.jyroscope.*;
import com.jyroscope.ros.tcpros.*;
import com.jyroscope.server.http.*;
import com.jyroscope.server.xmlrpc.*;
import java.net.*;

public class RosSlave {
    
    private final Name<RosTopic> topics;
    private final URI masterURI;
    private final URI slaveURI;
    private final String callerId;
    private final TCPROSServer tcpros;
    
    public RosSlave(URI masterURI, String localhostname, String callerId) {
        this.masterURI = masterURI;
        this.callerId = callerId;
        topics = new Name<>(name -> new RosTopic(name, this));
        tcpros = new TCPROSServer(topics, localhostname);
        
        XMLRPCSlave rpc = new XMLRPCSlave(this);
        SimpleHTTPServer server = new SimpleHTTPServer(localhostname, new XMLRPCService(new ReflectedAPI(rpc), true), false);
        slaveURI = server.getURI();
        
        // TODO add shutdown hooks
    }
    
    public URI getSlaveURI() {
        return slaveURI;
    }
    
    public String getCallerId() {
        return callerId;
    }
    
    public Name<RosTopic> getTopics() {
        return topics;
    }
    
    public boolean parameterUpdate(String key, Object value) {
        // TODO do something with this!
        return true;
    }
    
    public URI getMasterURI() {
        return masterURI;
    }
    
    public void shutdown(String msg) {
        Log.msg(this, "Shutdown Request: " + msg);
        tcpros.shutdown();
        // TODO propagate this to the rest of the system
    }
    
    public RosTopic getTopic(String namespace, String node, String topic) throws SystemException {
        Name<RosTopic> name = topics.parse(namespace).parse(node, topic);
        return (RosTopic)name.get();
    }
    
    public RosTransport requestTopic(String caller_id, RosTopic rostopic, XMLRPCArray protocols) throws CompatibilityException, SystemException {
        // Currently only TCPROS is supported
        checkTCPROS: {
            for (Object val : protocols) {
                if (val instanceof XMLRPCArray) {
                    XMLRPCArray array = (XMLRPCArray)val;
                    if ("TCPROS".equals(array.get(0)) && array.size() == 1)
                        break checkTCPROS;
                }
            }
            // Does not request TCPROS
            throw new CompatibilityException("Only TCPROS transports are accepted as slave requests"); 
        }
        // We just reuse the same TCP port
        return tcpros;
    }
}
