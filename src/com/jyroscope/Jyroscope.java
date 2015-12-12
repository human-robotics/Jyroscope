package com.jyroscope;

import com.jyroscope.annotations.*;
import com.jyroscope.local.*;
import com.jyroscope.ros.*;
import com.jyroscope.ros.types.*;
import com.jyroscope.types.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;

public class Jyroscope {

    private static class Subscription<T> {
        private Topic<T> topic;
        private Link<T> subscriber;
        private Subscription(Topic<T> topic, Link<T> subscriber) {
            this.topic = topic;
            this.subscriber = subscriber;
        }
    }    
    
    public static void addMsgSearchPath(String path) {
        RosTypes.addMsgSearchPath(path);
    }
    
    private ArrayList<TopicProvider<?>> providers;
    private IdentityHashMap<Object, List<Subscription<?>>> subscriptions;
    
    public Jyroscope() {
        providers = new ArrayList<>();
        subscriptions = new IdentityHashMap<>();
    }
    
    public void addRemoteMaster(String prefix, String uri, String localhost, String callerId) {
        RosTopicProvider topicProvider = new RosTopicProvider(prefix, uri, localhost, callerId);
        providers.add(topicProvider);
    }
    
    public void addLocalMaster(String prefix) {
        LocalTopicProvider topicProvider = new LocalTopicProvider(prefix);
        providers.add(topicProvider);
    }
    
    public Topic<?> getTopic(String name) {
        for (TopicProvider<?> provider : providers) {
            String prefix = provider.getPrefix();
            if (name.startsWith(prefix)) {
                name = name.substring(prefix.length());
                return provider.getTopic(name);
            }
        }
        // No provider found
        throw new IllegalArgumentException("Could not find an appropriate topic provider for: " + name);
    }

    public <T> T create(Class<? extends T> nodeClass) throws CreationException {
        try {
            return inject(nodeClass.newInstance());
        } catch (IllegalAccessException iae) {
            throw new CreationException("Could not access node " + nodeClass.getName() + " (it should be declared public and have a default/no-argument constructor)", iae);
        } catch (InstantiationException ie) {
            throw new CreationException("Could not instantiate node " + nodeClass.getName(), ie);
        }
    }
    
    public void create(Class<?>... nodeClasses) throws CreationException {
        for (Class<?> nodeClass : nodeClasses) {
            create(nodeClass);
        }
    }
        
    public <T> T inject(T object) throws CreationException {
        Class<?> nodeClass = object.getClass();
        try {
            // Publishers
            for (Field field : nodeClass.getDeclaredFields()) {
                Publish publisher = field.getAnnotation(Publish.class);
                if (publisher != null) {
                    int modifiers = field.getModifiers();
                    if (Modifier.isStatic(modifiers))
                        throw new CreationException("Publisher at " + field.toGenericString() + " must be non-static");
                    
                    if (!Modifier.isPublic(modifiers))
                        field.setAccessible(true);
                    
                    Link<?> link = createPublisher(publisher, field);
                    field.set(object, link);
                }
            }
            
            ArrayList<Method> initializers = new ArrayList<>();
            LinkedHashMap<Method,Repeat> repeaters = new LinkedHashMap<>();
            for (Method method : nodeClass.getDeclaredMethods()) {
                // Subscribers
                Subscribe subscriber = method.getAnnotation(Subscribe.class);
                if (subscriber != null) {
                    int modifiers = method.getModifiers();
                    if (!Modifier.isPublic(modifiers) || Modifier.isStatic(modifiers))
                        throw new CreationException("Subscriber at " + method.toGenericString() + " must be public and non-static");
                    
                    Link<?> link = createSubscriber(subscriber, method, object);
                    
                    try {
                        Topic<?> topic = getTopic(subscriber.value());
                        topic.subscribe((Link)link);
                        
                        List<Subscription<?>> currentSubscriptions = subscriptions.get(object);
                        if (currentSubscriptions == null)
                            subscriptions.put(object, currentSubscriptions = new ArrayList<>());
                        currentSubscriptions.add(new Subscription<>((Topic)topic, (Link)link));
                        
                    } catch (ConversionException ce) {
                        throw new CreationException("Could not convert message types for subscriber at " + method.toGenericString());
                    }
                    
                }
                
                // Initializer
                Init initializer = method.getAnnotation(Init.class);
                if (initializer != null) {
                    int modifiers = method.getModifiers();
                    if (!Modifier.isPublic(modifiers) || Modifier.isStatic(modifiers))
                        throw new CreationException("Initializer at " + method.toGenericString() + " must be public and non-static");
                    Parameter[] parameters = method.getParameters();
                    if (parameters.length != 0)
                        throw new CreationException("Initializer at " + method.toGenericString() + " must be non-static and take no parameters");
                    initializers.add(method);
                }
                    
                // Repeaters
                Repeat repeater = method.getAnnotation(Repeat.class);
                if (repeater != null) {
                    int modifiers = method.getModifiers();
                    if (!Modifier.isPublic(modifiers) || Modifier.isStatic(modifiers))
                        throw new CreationException("Repeater at " + method.toGenericString() + " must be public and non-static");
                    Parameter[] parameters = method.getParameters();
                    if (parameters.length != 0)
                        throw new CreationException("Repeater at " + method.toGenericString() + " must be non-static and take no parameters");
                    if (repeater.interval() != 0 && repeater.delay() != 0)
                        throw new CreationException("Repeater at " + method.toGenericString() + " cannot have both non-zero interval and non-zero delay");
                    if (repeater.interval() < 0 || repeater.delay() < 0)
                        throw new CreationException("Repeater at " + method.toGenericString() + " does not support non-negative intervals or delays");
                    if (repeater.count() < 0)
                        throw new CreationException("Repeater at " + method.toGenericString() + " cannot have negative count");
                    if (!Void.TYPE.equals(method.getReturnType()) && !Boolean.TYPE.equals(method.getReturnType()))
                        throw new CreationException("Repeater at " + method.toGenericString() + " must return void or boolean");
                    repeaters.put(method, repeater);
                }
            }
            
            // Initializers
            for (Method method : initializers) {
                try {
                    method.invoke(object);
                } catch (IllegalArgumentException | InvocationTargetException e) {
                    throw new CreationException("An error occured while calling node initializer " + method.toGenericString(), e);
                }
            }
            
            // Repeaters
            for (Map.Entry<Method,Repeat> repeater : repeaters.entrySet()) {
                Method method = repeater.getKey();
                Repeat repeat = repeater.getValue();
                boolean isDelay = repeat.delay() != 0;
                boolean isInterval = repeat.interval() != 0;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO: handle shutdown cleanly
                        int count = 0;
                        long start = System.currentTimeMillis();
                        while (repeat.count() == 0 || count < repeat.count()) {
                            count++;
                            try {
                                Object result = method.invoke(object);
                                
                                // Check if it returned false
                                if (result != null)
                                    if (!(Boolean)result)
                                        break;
                            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                                Log.exception(object, e, "An error occured when invoking the repeater");
                            }
                            try {
                                if (isDelay)
                                    Thread.sleep(repeat.delay());
                                else if (isInterval) {
                                    long now = System.currentTimeMillis();
                                    long sleep = repeat.interval() - (now - start);
                                    if (sleep > 0)
                                        Thread.sleep(sleep);
                                }
                            } catch (InterruptedException ie) {
                                // do nothing
                            }
                        }
                    }
                }).start();
            }
        } catch (IllegalAccessException iae) {
            throw new CreationException("Could not access node " + nodeClass.getName() + " (it should be declared public and have a default/no-argument constructor)", iae);
        }
        return object;
    }
    
    public Link<?> createPublisher(Publish publisher, Field field) throws CreationException {
        if (!Link.class.equals(field.getType()) && !Publisher.class.equals(field.getType()))
            throw new CreationException("Publisher at " + field.toGenericString() + " must be declared com.jyroscope.Link<T> where T is a message type");

        Type type = field.getGenericType();
        if (!(type instanceof ParameterizedType))
            throw new CreationException("Publisher at " + field.toGenericString() + " must be declared com.jyroscope.Link<T> where T is a message type");

        Type[] typeArgs = ((ParameterizedType)type).getActualTypeArguments();
        if (typeArgs.length != 1)
            throw new CreationException("Publisher at " + field.toGenericString() + " must be declared com.jyroscope.Link<T> where T is a message type");

        Class topicType = null;
        Type param = typeArgs[0];
        if (param instanceof Class)
            topicType = (Class)param;
        else if (param instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType)param).getRawType();
            if (rawType instanceof Class)
                topicType = (Class)rawType;
        }

        if (topicType == null)
            throw new UnsupportedClassVersionError("Unrecognized type parameter for publisher at " + field.toGenericString());

        try {

            return getTopic(publisher.value()).getPublisher(topicType);

        } catch (ConversionException ce) {
            throw new CreationException("Could not convert message types for publisher at " + field.toGenericString());
        }
    }

    
    public <T> Link<?> createSubscriber(Subscribe subscriber, Method method, T object) throws CreationException {
        Parameter[] parameters = method.getParameters();
        if (parameters.length != 1)
            throw new CreationException("Subscriber at " + method.toGenericString() + " must have exactly one parameter");
        
        Parameter parameter = parameters[0];
        final Class<? extends Object> messageType = parameter.getType();
        String path = subscriber.value();

        // TODO precompile this use of reflection
        return new Link<Object>() {

            @Override
            public Class<? extends Object> getType() {
                return messageType;
            }

            @Override
            public void handle(Object message) {
                try {
                    method.invoke(object, message);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    // TODO handle this better
                    throw new RuntimeException(e);
                }
            }
        };
    }
    
}
