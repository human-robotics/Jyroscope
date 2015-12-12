package com.jyroscope;

public interface Publisher<T> extends Link<T> {
    
    public void latch(T message);
    
    @Override
    public void handle(T message); // This is unnecessary but it helps when the IDEs highlight declared methods
    
}
