package com.jyroscope.types;

import com.jyroscope.*;
import java.util.*;
import java.util.concurrent.locks.*;

// TODO handle concurrent access
public class LinkManager {
    
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    
    private class Receive<D> implements Publisher<D> {
        
        private final Class<? extends D> fromType;
        private final boolean isLocal;
        private volatile boolean isLatched;
        private volatile D latchedValue;
        
        private Receive(Class<? extends D> fromType, boolean isLocal) {
            this.fromType = fromType;
            this.isLocal = isLocal;
        }

        @Override
        public Class<? extends D> getType() {
            return fromType;
        }

        @Override
        public void handle(D message) {
            if (isLocal)
                isLatched = false;
            process(message);
        }

        @Override
        public void latch(D message) {
            if (isLocal)
                synchronized (this) {
                    isLatched = true;
                    latchedValue = message;
                }
            process(message);
        }
        
        private void process(D message) {
            lock.readLock().lock();
            try {
                for (Map.Entry<Class<?>, Deliver<?>> entry : listeners.entrySet()) {
                    Deliver<?> deliver = entry.getValue();
                    if (deliver.hasClients(isLocal)) {
                        TypeConverter converter = TypeConverter.get(fromType, entry.getKey());
                        Object converted = converter.convert(message);
                        ((Deliver)deliver).handle(converted, isLocal);
                    }
                }
            } finally {
                lock.readLock().unlock();
            }
        }
    }
    
    private class Deliver<D> {
        
        private final Class<? extends D> to;
        private final ArrayList<Link<D>> local;
        private final ArrayList<Link<D>> remote;
        
        private Deliver(Class<? extends D> to) {
            this.to = to;
            this.local = new ArrayList<>();
            this.remote = new ArrayList<>();
        }
        
        private void add(Link<D> link, boolean isLocal) {
            if (isLocal)
                local.add(link);
            else
                remote.add(link);
        }
        
        private void remove(Link<D> link) {
            local.remove(link);
            remote.remove(link);
        }
        
        private boolean hasLocalClients() {
            return local.size() > 0;
        }
        
        private boolean hasClients(boolean forMessageFromLocal) {
            return local.size() > 0 || (forMessageFromLocal && remote.size() > 0);
        }
        
        private void handle(D message, boolean isLocal) {
            if (local.size() > 0)
                for (Link<D> link : local)
                    link.handle(message);
            if (isLocal && remote.size() > 0)
                for (Link<D> link : remote)
                    link.handle(message);
        }

        private boolean isEmpty() {
            return local.isEmpty() && remote.isEmpty();
        }

    }
    
    private final HashMap<Class<?>, Receive<?>> publishers;
    private final HashMap<Class<?>, Deliver<?>> listeners;
    
    public LinkManager() {
        publishers = new HashMap<>();
        listeners = new HashMap<>();
    }

    public synchronized <D> Publisher<D> getPublisher(Class<? extends D> from, boolean isLocal) throws ConversionException {
        Receive<?> publisher;
        lock.readLock().lock();
        try {
            publisher = publishers.get(from);
        } finally {
            lock.readLock().unlock();
        }
            
        if (publisher == null) {
            // ensure that converters are pre-cached
            // throw an exception (don't add the publisher) if it cannot be cached
            for (Class<?> to : listeners.keySet()) {
                if (isLocal || listeners.get(to).hasLocalClients())
                    TypeConverter.precompile(from, to);
            }

            lock.writeLock().lock();
            try {
                publishers.put(from, publisher = new Receive<>(from, isLocal));
            } finally {
                lock.writeLock().unlock();
            }
        }
        return (Publisher<D>)publisher;
    }
    
    public <D> void subscribe(Link<D> link, boolean isLocal) throws ConversionException {
        ArrayList<D> latched;
        lock.readLock().lock();
        try {
            Class<? extends D> to = link.getType();
            Deliver<D> deliver = (Deliver<D>)listeners.get(to);
            if (deliver == null) {
                // ensure that converters are pre-cached
                // throw an exception (don't add the publisher) if it cannot be cached
                for (Class<?> from : publishers.keySet())
                    TypeConverter.precompile(from, to);

                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    listeners.put(to, deliver = new Deliver<>(to));
                } finally {
                    lock.writeLock().unlock();
                    lock.readLock().lock();
                }

            }
            deliver.add(link, isLocal);

            // Pass latched messages to new subscribers
            // Note that we need to convert them again because we don't cache the conversions
            latched = new ArrayList<>();
            for (Receive<?> r : publishers.values())
                synchronized (r) {
                    if (r.isLatched) {
                        TypeConverter converter = TypeConverter.get(r.fromType, to);
                        Object converted = converter.convert(r.latchedValue);
                        latched.add((D)converted);
                    }
                }
        } finally {
            lock.readLock().unlock();
        }
        
        for (D message : latched)
            link.handle(message);

    }
    
    public <D> void unsubscribe(Link<D> link) {
        lock.writeLock().lock();
        try {
            Class<? extends D> to = link.getType();
            Deliver<D> deliver = (Deliver<D>)listeners.get(to);
            if (deliver != null) {
                deliver.remove(link);
                if (deliver.isEmpty())
                    listeners.remove(to);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public boolean hasSubscribers() {
        lock.readLock().lock();
        try {
            return listeners.size() > 0;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public boolean hasLocalSubscribers() {
        lock.readLock().lock();
        try {
            if (listeners.isEmpty())
                return false;
            for (Deliver<?> deliver : listeners.values())
                if (deliver.hasClients(true))
                    return true;
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }

}
