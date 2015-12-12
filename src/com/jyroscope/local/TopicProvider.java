package com.jyroscope.local;

public interface TopicProvider<T> {

    public String getPrefix();
    public Topic<T> getTopic(String name);
    
}
