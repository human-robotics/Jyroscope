package com.jyroscope.local;

import com.jyroscope.*;
import com.jyroscope.types.*;

public interface Topic<T> {
    
    public void subscribe(Link<T> subscriber) throws ConversionException;
    public void unsubscribe(Link<T> subscriber);
    public <D> Publisher<D> getPublisher(Class<? extends D> type) throws ConversionException;

}
