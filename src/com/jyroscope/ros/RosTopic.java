package com.jyroscope.ros;

import com.jyroscope.Link;
import com.jyroscope.Name;
import java.util.*;

import com.jyroscope.*;
import com.jyroscope.local.*;
import com.jyroscope.ros.types.*;
import com.jyroscope.server.xmlrpc.*;
import com.jyroscope.types.*;
import com.jyroscope.util.*;
import java.io.*;
import java.net.*;

// TODO see if the synchronization locks can be freed up a bit
public class RosTopic<T> implements Topic<T> {
    
    private Class<? extends T> messageType;
    
    private final Name<RosTopic> name;

    private final RosSlave slave;
    
    private final LinkManager links;
    
    
    private final HashMap<URI,RosNode> remotePublishers;
    private final ArrayList<Link<RosMessage>> remoteSubscribers;
    
//    private final ArrayList<Link<?>> localPublishers;
//    private final ArrayList<Link<T>> localSubscribers;
    
    private boolean isRegisteredPublisher;
    private boolean isRegisteredSubscriber;
    private boolean isConnectedToRemote;
    
    private Link<RosMessage> remoteMessageHandler;
   
//    private HashMap<URI, RosNode> rosPublishers;
//    private HashSet<String> publisherCount;
//    private HashSet<String> subscriberCount;
//    private Object lock = new Object() {};
//    private boolean isPublished;
//    private boolean isSubscribed;
    
    public RosTopic(Name<RosTopic> name, RosSlave slave) {
        this.name = name;
        this.slave = slave;
        
        this.remotePublishers = new HashMap<>();
        this.remoteSubscribers = new ArrayList<>();
        
//        this.localSubscribers  = new ArrayList<>();
//        this.localPublishers = new ArrayList<>();
        
        this.isRegisteredPublisher = false;
        this.isRegisteredSubscriber = false;
        this.isConnectedToRemote = false;
        
        this.links = new LinkManager();
        
        
        
//        rosPublishers = new HashMap<URI, RosNode>();
//        publisherCount = new HashSet<String>();
//        subscriberCount = new HashSet<String>();
//        isPublished = false;
//        isSubscribed = false;
    }
    
    public Name<RosTopic> getName() {
        return name;
    }
    
    public String getRosType() throws ConversionException {
        return RosTypeConverterFactory.getRosType(messageType).getName();
    }
    
    public synchronized void addRemoteSubscriber(Link<RosMessage> subscriber) throws ConversionException {
        remoteSubscribers.add(subscriber);
        links.subscribe(subscriber, false);
    }
    
    public synchronized void removeRemoteSubscriber(Link<RosMessage> subscriber) {
        remoteSubscribers.remove(subscriber);
        links.unsubscribe(subscriber);
    }
    
//    public synchronized Collection<RosNode> addRemoteSubscriber(final RosNode subscriber) throws SystemException, IncompatibleTypeException {
//        // TODO in the event of an exception, unwind all the publishers it is now subscribing from
//        Collection<RosNode> currentPublishers = new ArrayList<>(remotePublishers.values());
//        subscriber.initialPublishers(this, currentPublishers);
//        subscriber.addNodeListener(new NodeListener() {
//            @Override
//            public void closed() throws SystemException {
//                removeRemoteSubscriber(subscriber);
//            }
//        });
//        remoteSubscribers.put(subscriber.getId(), subscriber);
//        return currentPublishers;
//    }
//    
//    public synchronized int removeRemoteSubscriber(RosNode subscriber) throws SystemException {
//        return null != remoteSubscribers.remove(subscriber.getId()) ? 1 : 0;
//    }
//    
//    public synchronized int removeRemotePublisher(RosNode publisher) throws SystemException {
//        int rval = null != remotePublishers.remove(publisher.getId()) ? 1 : 0;
//        Collection<RosNode> change = Arrays.asList(new RosNode[] { publisher });
//        
//        for (RosNode subscriber : remoteSubscribers.values())
//            subscriber.publishersRemoved(this, change);
//        return rval;
//    }

    
//    private void notifySubscriptions(boolean hasSubscribers) throws SystemException {
//        try {
//            if (!isSubscribed && hasSubscribers) {
//                XMLRPCClient master = new XMLRPCClient(slave.getMasterURI());
//                Object result = master.call("registerSubscriber", new XMLRPCArray(new Object[] {slave.getCallerId(), name.toNameString(), RosBridge.getRosMessageType(this.getType()).getName(), slave.getSlaveURI().toASCIIString()}));
//                isSubscribed = true;
//                XMLRPCArray resultList = (XMLRPCArray)result;
//                if (((Integer)resultList.get(0)).intValue() != 1)
//                    throw new SystemException("Master returned error when trying to subscribe to topic " + name.toNameString());
//                XMLRPCArray publishers = (XMLRPCArray)resultList.get(2);
//                publisherUpdate(publishers);
//                
//            } else if (isSubscribed && !hasSubscribers) {
//                XMLRPCClient master = new XMLRPCClient(slave.getMasterURI());
//                Object result = master.call("unregisterSubscriber", new XMLRPCArray(new Object[] {slave.getCallerId(), name.toNameString(), slave.getSlaveURI().toASCIIString()}));
//                isSubscribed = false;
//                XMLRPCArray resultList = (XMLRPCArray)result;
//                if (((Integer)resultList.get(0)).intValue() != 1)
//                    Log.msg(this, "Unregister subscriber failed on server for topic " + name.toNameString());
//            }
//        } catch (IOException ioe) {
//            throw new SystemException(ioe);
//        } catch (XMLRPCException xre) {
//            throw new SystemException(xre);
//        } catch (FormatException fe) {
//            throw new SystemException(fe);
//        }
//    }
//    
    
//   Add local publisher    
//            if (!(publisher instanceof RosNode)) {
//            synchronized (lock) {
//                publisherCount.add(publisher.getId());
//            }
//            notifyPublications(true);
//        }
    
    
//    private void notifyPublications(boolean hasPublishers) throws SystemException {
//        try {
//            if (!isPublished && hasPublishers) {
//                XMLRPCClient master = new XMLRPCClient(slave.getMasterURI());
//                Object result = master.call("registerPublisher", new XMLRPCArray(new Object[] {slave.getCallerId(), name.toNameString(), RosBridge.getRosMessageType(this.getType()).getName(), slave.getSlaveURI().toASCIIString()}));
//                isPublished = true;
//                XMLRPCArray resultList = (XMLRPCArray)result;
//                if (((Integer)resultList.get(0)).intValue() != 1)
//                    throw new SystemException("Master returned error when trying to publish to topic " + name.toNameString());
//                // we don't need to handle the returned list of subscribers because the server will notify the subscribers for us
//                
//            } else if (isPublished && !hasPublishers) {
//                XMLRPCClient master = new XMLRPCClient(slave.getMasterURI());
//                Object result = master.call("unregisterPublisher", new XMLRPCArray(new Object[] {slave.getCallerId(), name.toNameString(), slave.getSlaveURI().toASCIIString()}));
//                isPublished = false;
//                XMLRPCArray resultList = (XMLRPCArray)result;
//                if (((Integer)resultList.get(0)).intValue() != 1)
//                    Log.msg(this, "Unregister publisher failed on server for topic " + name.toNameString());
//            }
//        } catch (IOException ioe) {
//            throw new SystemException(ioe);
//        } catch (XMLRPCException xre) {
//            throw new SystemException(xre);
//        }
//    }
//

    
//    public int removeLocalPublisher(RosNode publisher) throws SystemException {
//        if (!(publisher instanceof RosRemote)) {
//            boolean hasPublishers;
//            synchronized (lock) {
//                publisherCount.remove(publisher.getId());
//                hasPublishers = publisherCount.size() > 0;
//            }
//            notifyPublications(hasPublishers);
//        }
//        return rval;
//    }
    
    
//    @Override
//    public Collection<Publisher> addSubscriber(RosNode subscriber) throws SystemException, IncompatibleTypeException {
//        Collection<Publisher> result = super.addSubscriber(subscriber);
//        if (!(subscriber instanceof RosRemote)) {
//            synchronized (lock) {
//                subscriberCount.add(subscriber.getId());
//            }
//            notifySubscriptions(true);
//        }
//        return result;
//    }
    
//    @Override
//    public int removeSubscriber(Subscriber subscriber) throws SystemException {
//        int rval = super.removeSubscriber(subscriber);
//        if (!(subscriber instanceof RosRemote)) {
//            boolean hasSubscribers;
//            synchronized (lock) {
//                subscriberCount.remove(subscriber.getId());
//                hasSubscribers = subscriberCount.size() > 0;
//            }
//            notifySubscriptions(hasSubscribers);
//        }
//        return rval;
//    }
    
    public synchronized void publisherUpdate(XMLRPCArray slaves) throws FormatException, SystemException {
        HashSet<URI> uris = new HashSet<>();
        for (Object value : slaves)
            try {
                URI uri = new URI((String)value);
                if (!slave.getSlaveURI().equals(uri)) // Ignore notifications about myself
                    uris.add(uri);
            } catch (URISyntaxException use) {
                throw new FormatException("Bad URI format: " + String.valueOf(value));
            }
        

        // Add the new URIs
        for (URI uri : uris) {
            if (!remotePublishers.containsKey(uri)) {
                RosNode remote = new RosNode(null, uri, slave);
                try {
                    remote.connect(this);
                } catch (ConversionException ce) {
                    Log.exception(this, ce, "Cannot convert ROS message to registered subscribers");
                }
                remotePublishers.put(uri, remote);
            }
        }
        // Remove the old URIs
        for (Iterator<Map.Entry<URI,RosNode>> it = remotePublishers.entrySet().iterator(); it.hasNext(); ) { 
            Map.Entry<URI,RosNode> entry = it.next();
            if (!uris.contains(entry.getKey())) {
                RosNode remote = entry.getValue();
                it.remove();
                remote.disconnect(this);
            }
        }
    }
    
    private void registerAsSubscriber() {
        Do.later(new Runnable() {
            @Override
            public void run() {
                try {
                    String rosType = getRosType();
                    isRegisteredSubscriber = true;
                    XMLRPCClient master = new XMLRPCClient(slave.getMasterURI());
                    Object result = master.call("registerSubscriber", new XMLRPCArray(new Object[] {slave.getCallerId(), name.toNameString(), rosType, slave.getSlaveURI().toASCIIString()}));
                    XMLRPCArray resultList = (XMLRPCArray)result;
                    if ((Integer)resultList.get(0) != 1)
                        throw new SystemException("Master returned error when trying to subscribe to topic " + name.toNameString());
                    XMLRPCArray publishers = (XMLRPCArray)resultList.get(2);
                    publisherUpdate(publishers);

                } catch (IOException | XMLRPCException | FormatException | SystemException | ConversionException e) {
                    // TODO better handle this error
                    e.printStackTrace();
                }
            }
        });
    }

    private void unregisterAsSubscriber() {
        Do.later(new Runnable() {
            @Override
            public void run() {
                try {
                    XMLRPCClient master = new XMLRPCClient(slave.getMasterURI());
                    Object result = master.call("unregisterSubscriber", new XMLRPCArray(new Object[] {slave.getCallerId(), name.toNameString(), slave.getSlaveURI().toASCIIString()}));
                    XMLRPCArray resultList = (XMLRPCArray)result;
                    if ((Integer)resultList.get(0) != 1)
                        Log.msg(this, "Unregister subscriber failed on server for topic " + name.toNameString());
                    else
                        isRegisteredSubscriber = false;
                } catch (IOException | XMLRPCException e) {
                    // TODO better handle this error
                    e.printStackTrace();
                }
            }
        });
    }
    
    private void registerAsPublisher() {
        Do.later(new Runnable() {
            @Override
            public void run() {
                try {
                    String rosType = getRosType();
                    isRegisteredPublisher = true;
                    XMLRPCClient master = new XMLRPCClient(slave.getMasterURI());
                    Object result = master.call("registerPublisher", new XMLRPCArray(new Object[] {slave.getCallerId(), name.toNameString(), rosType, slave.getSlaveURI().toASCIIString()}));
                    XMLRPCArray resultList = (XMLRPCArray)result;
                    if ((Integer)resultList.get(0) != 1)
                        throw new SystemException("Master returned error when trying to publish to topic " + name.toNameString());
                    else
                        isRegisteredPublisher = true;
                    
                    // TODO do we need to handle the returned list of subscribers???
                    // we don't need to handle the returned list of subscribers because the server will notify the subscribers for us

                } catch (IOException | XMLRPCException | SystemException | ConversionException e) {
                    // TODO better handle this error
                    e.printStackTrace();
                }
            }
        });
    }
    
    private void unregisterAsPublisher() {
        Do.later(new Runnable() {
            @Override
            public void run() {
                try {
                    isRegisteredPublisher = false;
                    XMLRPCClient master = new XMLRPCClient(slave.getMasterURI());
                    Object result = master.call("unregisterPublisher", new XMLRPCArray(new Object[] {slave.getCallerId(), name.toNameString(), slave.getSlaveURI().toASCIIString()}));
                    XMLRPCArray resultList = (XMLRPCArray)result;
                    if ((Integer)resultList.get(0) != 1)
                        Log.msg(this, "Unregister publisher failed on server for topic " + name.toNameString());
                    
                    // TODO better handle a failed unregister
                    
                } catch (IOException | XMLRPCException e) {
                    // TODO better handle this error
                    e.printStackTrace();
                }
            }
        });
    }
    
    private void connectToRemote() throws ConversionException {
        if (!isConnectedToRemote)
            for (RosNode node : remotePublishers.values())
                node.connect(this);
        isConnectedToRemote = true;
    }
    
    private void disconnectToRemote() {
        if (isConnectedToRemote)
            for (RosNode node : remotePublishers.values())
                node.disconnect(this);
        isConnectedToRemote = false;
    }
    
    @Override
    public synchronized void subscribe(Link<T> subscriber) throws ConversionException {
        links.subscribe(subscriber, true);
//        localSubscribers.add(subscriber);
        if (messageType == null)
            messageType = subscriber.getType();
        connectToRemote();
        registerAsSubscriber();
    }

    @Override
    public synchronized void unsubscribe(Link<T> subscriber) {
        links.unsubscribe(subscriber);
//        localSubscribers.remove(subscriber);
        if (!links.hasLocalSubscribers()) {
            disconnectToRemote();
            unregisterAsSubscriber();
        }
    }

    @Override
    public <D> Publisher<D> getPublisher(Class<? extends D> type) throws ConversionException {
        if (messageType == null)
            messageType = (Class<? extends T>)type;
        
        Publisher<D> publisher = links.getPublisher(type, true);
        
        if (!isRegisteredPublisher)
            registerAsPublisher();
        
        return publisher;
    }
    
    public Link<RosMessage> getRemotePublisher() throws ConversionException {
        return links.getPublisher(RosMessage.class, false);
    }
    
}
