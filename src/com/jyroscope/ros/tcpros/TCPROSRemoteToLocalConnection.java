package com.jyroscope.ros.tcpros;

import com.jyroscope.ros.types.RosMessageType;
import java.io.*;
import java.net.*;
import java.nio.*;

import com.jyroscope.*;
import com.jyroscope.ros.*;

public class TCPROSRemoteToLocalConnection {
    
    private String host;
    private int port;
    
    private String callerid;
    private String topic;
    private RosMessageType type;
    private boolean tcpNoDelay;
    
    private OutputStream os;
    private InputStream is;
    private Socket socket;
    
    public TCPROSRemoteToLocalConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    public void setParameters(String callerid, String topic, RosMessageType type) {
        this.callerid = callerid;
        this.topic = topic;
        this.type = type;
    }
    
    public void setTCPNoDelay() {
        tcpNoDelay = true;
    }
    
    public boolean connect() throws IOException {
        RosMessage buffer = new RosMessage();
        
        socket = new Socket(host, port);
        os = socket.getOutputStream();
        
        TCPROSHeader request = new TCPROSHeader();
        request.putHeader("callerid", callerid);
        request.putHeader("topic", topic);
        request.putHeader("md5sum", type.getHash());
        request.putHeader("type", type.getName());
        if (tcpNoDelay)
            request.putHeader("tcp_nodelay", "1");
        request.render(buffer);
        buffer.writeOut(os);
        os.flush();
        
        is = socket.getInputStream();
        
        TCPROSHeader acknowledge = new TCPROSHeader();
        if (buffer.readIn(is)) {
            acknowledge.parse(buffer);
            return true;
        } else
            return false;
    }
    
    public boolean read(RosMessage buffer) throws IOException, FormatException {
        return buffer.readIn(is);
    }
    
    public void close() throws IOException {
        is.close();
        os.close();
        socket.close();
    }

}
