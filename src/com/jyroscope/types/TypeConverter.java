package com.jyroscope.types;

import com.jyroscope.local.types.*;
import com.jyroscope.ros.types.*;
import java.util.*;

public abstract class TypeConverter<S,D> {

    private static final ArrayList<TypeConverterFactory> factories;
    private static final Map<Class<?>, Map<Class<?>, TypeConverter<?,?>>> map;
    
    static {
        factories = new ArrayList<>();
        factories.add(new RosTypeConverterFactory());
        factories.add(new JavaTypeConverterFactory());
        map = Collections.synchronizedMap(new HashMap<>());
    }

    
    public static <S,D> TypeConverter get(Class<? extends S> from, Class<? extends D> to) {
        return map.get(from).get(to);
    }

    public static synchronized void precompile(Class<?> from, Class<?> to) throws ConversionException {
        Map<Class<?>, TypeConverter<?, ?>> toMap = map.get(from);
        if (toMap == null)
            map.put(from, toMap = Collections.synchronizedMap(new HashMap<>()));
        TypeConverter<?, ?> converter = toMap.get(to);
        if (converter == null) {
            
            getConverter:
                for (TypeConverterFactory factory : factories) {
                    converter = factory.get(from, to);
                    if (converter != null)
                        break getConverter;
                }
            
            if (converter != null)
                toMap.put(to, converter);
            else
                throw new ConversionException("No conversion between " + from.getName() + " and " + to.getName());
        }
    }
    
    public abstract D convert(S source);
        
}
