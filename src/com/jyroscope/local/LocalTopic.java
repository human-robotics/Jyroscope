package com.jyroscope.local;

import com.jyroscope.*;
import com.jyroscope.util.*;
import java.util.*;

public class LocalTopic<T> implements Topic<T> {
    
    private Set<Class<? extends T>> types;
    private List<Link<T>> subscribers;
    private HashMap<Publisher<T>, T> latched;
    private Name<LocalTopic> name;
    
    public LocalTopic(Name<LocalTopic> name) {
        this.name = name;
        this.subscribers = new ArrayList<>();
        this.types = new HashSet<>();
        this.latched = new HashMap<>();
    }

    @Override
    public synchronized void subscribe(Link<T> subscriber) {
        types.add(subscriber.getType());
        subscribers.add(subscriber);
        
        ArrayList<T> copy;
        synchronized (latched) {
            copy = new ArrayList<>(latched.values());
        }
        for (T message : copy) {
            subscriber.handle(message);
        }
    }

    @Override
    public synchronized void unsubscribe(Link<T> subscriber) {
        subscribers.remove(subscriber);
    }

    @Override
    public <D> Publisher<D> getPublisher(Class<? extends D> type) {
        return new Publisher<D>() {

            @Override
            public Class<? extends D> getType() {
                return type;
            }

            @Override
            public void handle(D message) {
                synchronized (latched) {
                    latched.remove((Publisher<T>)this);
                }
                process(message);
            }
            
            @Override
            public void latch(D message) {
                synchronized (latched) {
                    latched.put((Publisher<T>)this, (T)message);
                }
                process(message);
            }
            
            private void process(D message) {
                // TODO do type conversion rather than just a naive cast
                for (Link<T> link : subscribers)
                    Do.deliver(message, (Link<D>)link);
            }
        };
    }

}
