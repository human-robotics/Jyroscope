package com.jyroscope.ros;

import com.jyroscope.*;
import com.jyroscope.local.*;
import java.net.*;

public class RosTopicProvider implements TopicProvider {

    private final String prefix;
    private final RosSlave slave;
    
    public RosTopicProvider(String prefix, String uri, String localhost, String callerId) {
        this.prefix = prefix + ":";
        URI masterUri = URI.create(uri);
        slave = new RosSlave(masterUri, localhost, callerId);
    }
    
    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public RosTopic getTopic(String name) {
        try {
            return slave.getTopics().parse(name).get();
        } catch (SystemException se) {
            // This should never occur
            throw new RuntimeException(se);
        }
    }
}
