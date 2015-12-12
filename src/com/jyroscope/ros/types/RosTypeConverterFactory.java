package com.jyroscope.ros.types;

import com.jyroscope.*;
import com.jyroscope.annotations.*;
import com.jyroscope.ros.*;
import com.jyroscope.types.*;
import com.jyroscope.types.TypeConverterHelperCollection;
import com.jyroscope.util.*;
import java.lang.reflect.*;
import java.util.*;

public class RosTypeConverterFactory implements TypeConverterFactory {

    private static final String ROS_MESSAGE = "com.jyroscope.ros.RosMessage";

    private static final String readArraySize = "int #t = #1.getInt(); #= #t";
    private static final String startArrayReadLoop = "#1[] #t = new #1[#2]; for (int i=0; i<#t.length; i++) { #= #t";
    private static final String setArrayReadLoop = "#1[i] = #2;";
    private static final String closeArrayReadLoop = "} #= #1";
    
    private static final String skipWrite = "#1.zero(#2);";
    private static final String skipRead = "#1.skip(#2);";
    private static final String startSkipLoop = "for (int i=0; i<#1; i++) {";
    private static final String endSkipLoop = "}";
    private static final String skipMultipy = "#1.skip(#2 * #3);";
    private static final String skipReadString = "#1.skip(#1.getInt());";
    
    private static final String writeArrayCopy = "#1[] #t = #2; #= #t";
    private static final String writeArraySize = "#1.putInt(#2.length); #= #2.length";
    private static final String startArrayWriteLoop = "for (int i=0; i<#1; i++) { #2 #t = #3[i]; #= #t";
    private static final String closeArrayWriteLoop = "}";
    private static final String call = "#1;";
    
    private final TypeConverterHelperCollection<RosType, Class<?>> converters;
    
    public RosTypeConverterFactory() {
        converters = new TypeConverterHelperCollection<>();
        converters.add(new RosConvertPrimitive());
        converters.add(new RosConvertString());
        converters.add(new RosConvertListPrimitive());
    }
        
    @Override
    public <S,D> TypeConverter<S,D> get(Class<? extends S> from, Class<? extends D> to) throws ConversionException {
        if (RosMessage.class.isAssignableFrom(from))
            return (TypeConverter<S,D>)readRos(to);
        else if (RosMessage.class.isAssignableFrom(to))
            return (TypeConverter<S,D>)writeRos(from);
        else
            return null;
    }
    
    public static RosMessageType getRosType(Class<?> javaType) throws ConversionException {
        Message annotation = javaType.getAnnotation(Message.class);
        if (annotation == null)
            throw new ConversionException("Cannot convert between ROS type and " + javaType.getName() + " (missing @Message annotation)");
            
        String rosTypeName = annotation.value();
        RosMessageType rosType = RosTypes.getMessageType(rosTypeName);
        if (rosType == null)
            throw new ConversionException("Unrecognized ROS type " + rosTypeName + " for Java class " + javaType.getName());
        
        return rosType;
    }
        
    public <D> TypeConverter<RosMessage,D> readRos(Class<? extends D> to) throws ConversionException {
        RosMessageType fromType = getRosType(to);
        BeanType toType = BeanType.load(to);
        TypeConverterBuilder typeConverterBuilder = new TypeConverterBuilder(ROS_MESSAGE, toType.getName());
        String result = convertRos(typeConverterBuilder, null, to.getName(), typeConverterBuilder.getInput(), fromType, to);
        typeConverterBuilder.setOutput(result);
        String newClass = typeConverterBuilder.getName();
        String newSource = typeConverterBuilder.getSource();
        Log.finest(RosTypeConverterFactory.class, newSource);
        return (TypeConverter<RosMessage, D>)SourceLoader.create(newClass, newSource);
    }
    
    
    public <S> TypeConverter<S, RosMessage> writeRos(Class<? extends S> from) throws ConversionException {
        RosMessageType to = getRosType(from);
        BeanType fromType = BeanType.load(from);
        TypeConverterBuilder typeConverterBuilder = new TypeConverterBuilder(fromType.getName(), ROS_MESSAGE);
        
        MethodBuilder method = typeConverterBuilder.createMethod(ROS_MESSAGE, fromType.getName());
        // TODO cache this and initialize better
        String rosMessage = method.apply(ROS_MESSAGE + " #t = new " + ROS_MESSAGE + "(); #= #t");
        convertJava(typeConverterBuilder, method, to.getName(), rosMessage, method.getArg(0), from, to);
        method.add(method.apply("#1.flip();", rosMessage));
        method.setResult(rosMessage);
        
        typeConverterBuilder.setOutput(method.getInvocation(typeConverterBuilder.getInput()));
        
        String newClass = typeConverterBuilder.getName();
        String newSource = typeConverterBuilder.getSource();
        Log.finest(RosTypeConverterFactory.class, newSource);
        return (TypeConverter<S, RosMessage>)SourceLoader.create(newClass, newSource);
    }
    
    private void skipRos(MethodBuilder currentMethod, String rosMessage, RosType from) throws ConversionException {
        // Is it a constant skip amount?
        int totalSkip = from.getSize();
        if (totalSkip > 0) {
            currentMethod.add(currentMethod.apply(skipRead, rosMessage, String.valueOf(totalSkip)));
            return;
        } else if (totalSkip == 0)
            return;
        
        // otherwise deal with the special cases
        else if (from instanceof RosMessageType) {
            RosMessageType fromType = (RosMessageType)from;
            totalSkip = 0;
            for (Map.Entry<String, RosType> field : fromType.getFields().entrySet()) {
                RosType next = field.getValue();
                int nextSkip = next.getSize();
                if (nextSkip == -1) {
                    if (totalSkip > 0) {
                        currentMethod.add(currentMethod.apply(skipRead, rosMessage, String.valueOf(totalSkip)));
                        totalSkip = 0;
                    }
                    skipRos(currentMethod, rosMessage, next);
                } else
                    totalSkip += nextSkip;
            }
            if (totalSkip > 0)
                currentMethod.add(currentMethod.apply(skipRead, rosMessage, String.valueOf(totalSkip)));
            return;
            
        } else if (from instanceof RosListType) {
            RosListType fromType = (RosListType)from;
            RosType baseType = fromType.getBaseType();
            int length = fromType.getDeclaredLength();
            String lengthValue;
            if (length == -1)
                lengthValue = currentMethod.apply(readArraySize, rosMessage);
            else
                lengthValue = String.valueOf(length);
                
            int baseSize = baseType.getSize();

            if (baseSize == -1) {
                currentMethod.add(currentMethod.apply(startSkipLoop, lengthValue));
                skipRos(currentMethod, rosMessage, baseType);
                currentMethod.add(endSkipLoop);
            } else
                currentMethod.add(currentMethod.apply(skipMultipy, rosMessage, lengthValue, String.valueOf(baseSize)));
            
            return;
            
        } else if (from instanceof RosStringType) {
            currentMethod.add(currentMethod.apply(skipReadString, rosMessage));
            return;
        }
        
        // This should never happen
        throw new IllegalArgumentException("Unable to skip over ROS field of type " + from.getName());
    }
    
    private String convertRos(TypeConverterBuilder builder, MethodBuilder currentMethod, String currentContext, String rosMessage, RosType from, Class<?> to) throws ConversionException {
        String reader = converters.getReader(from, to);
        if (reader != null) {
            return currentMethod.apply(reader, rosMessage);
        } else if (from instanceof RosMessageType) {
            RosMessageType fromType = (RosMessageType)from;
            BeanType toType = BeanType.load(to);
            MethodBuilder method = builder.createMethod(toType.getName(), ROS_MESSAGE);
            method.setResult(Template.apply(null, "new #1()", toType.getName()));
            for (Map.Entry<String, RosType> field : fromType.getFields().entrySet()) {
                String name = field.getKey();
                BeanType.Property setter = toType.getSetter(name);
                if (setter != null) {
                    Class<?> targetType = setter.getType();
                    String setterTemplate = createSetterTemplate(setter);

                    String read = convertRos(builder, method, currentContext + "." + name, method.getArg(0), field.getValue(), targetType);
                    method.add(method.apply(setterTemplate, method.getResult(), read));
                } else {
                    Log.warn(RosTypeConverterFactory.class, "No setter for " + currentContext + "." + name + ", skipping");
                    skipRos(method, method.getArg(0), field.getValue());
                }
            }
            return method.getInvocation(rosMessage);    
        } else if (from instanceof RosListType && to.isArray()) {
            RosListType fromType = (RosListType)from;
            RosType baseType = fromType.getBaseType();
            int length = fromType.getDeclaredLength();
            String lengthValue;
            if (length == -1)
                lengthValue = currentMethod.apply(readArraySize, rosMessage);
            else
                lengthValue = String.valueOf(length);
            
            Class<?> elementType = to.getComponentType();
            String elementTypeName = elementType.getCanonicalName();
            if (elementTypeName != null) {
                String loopVar = currentMethod.apply(startArrayReadLoop, elementTypeName, lengthValue);
                currentMethod.add(currentMethod.apply(setArrayReadLoop, loopVar, convertRos(builder, currentMethod, currentContext + "[]", rosMessage, baseType, elementType)));
                return currentMethod.apply(closeArrayReadLoop, loopVar);
            }
        }
            
        throw new ConversionException("No cast from " + from.getName() + " to " + to.getName() + " for " + currentContext);
    }
    
    private void convertJava(TypeConverterBuilder builder, MethodBuilder currentMethod, String currentContext, String rosMessage, String object, Class<?> from, RosType to) throws ConversionException {
        String writer = converters.getWriter(from, to);
        
        if (writer != null) {
            currentMethod.add(currentMethod.apply(writer, rosMessage, object));
            return;
            
        } else if (to instanceof RosMessageType) {
            RosMessageType toType = (RosMessageType)to;
            BeanType fromType = BeanType.load(from);
            MethodBuilder method = builder.createMethod(null, fromType.getName(), ROS_MESSAGE);
            for (Map.Entry<String, RosType> field : toType.getFields().entrySet()) {
                String name = field.getKey();
                BeanType.Property getter = fromType.getGetter(name);
                if (getter != null) {
                    Class<?> sourceType = getter.getType();
                    String getterTemplate = createGetterTemplate(getter);
                    String value = method.apply(getterTemplate, method.getArg(0));
                    convertJava(builder, method, currentContext + "." + name, method.getArg(1), value, sourceType, field.getValue());
                } else {
                    Log.warn(RosTypeConverterFactory.class, "No setter for " + currentContext + "." + name + ", skipping");
                    skipJava(method, method.getArg(1), field.getValue());
                }
            }
            currentMethod.add(currentMethod.apply(call, method.getInvocation(object, rosMessage)));
            return;
            
        } else if (to instanceof RosListType) {
            RosListType toType = (RosListType)to;
            RosType baseType = toType.getBaseType();
            int length = toType.getDeclaredLength();
            
            if (from.isArray()) {
                String lengthValue;
                
                Class<?> elementType = from.getComponentType();
                String elementTypeName = elementType.getCanonicalName();
                
                String cachedValue = currentMethod.apply(writeArrayCopy, elementTypeName, object);
                
                if (length == -1)
                    lengthValue = currentMethod.apply(writeArraySize, rosMessage, cachedValue);
                else
                    lengthValue = String.valueOf(length);
                
                if (elementTypeName != null) {
                    String loopVar = currentMethod.apply(startArrayWriteLoop, lengthValue, elementTypeName, cachedValue);
                    convertJava(builder, currentMethod, currentContext + "[]", rosMessage, loopVar, elementType, baseType);
                    currentMethod.add(closeArrayWriteLoop);
                    return;
                }
            }
        }
        
        throw new ConversionException("No cast from " + from.getName() + " to " + to.getName() + " for " + currentContext);
    }
    
    private void skipJava(MethodBuilder currentMethod, String rosMessage, RosType to) throws ConversionException {
        int minimumSize = to.getMinimumSize();
        currentMethod.add(currentMethod.apply(skipWrite, rosMessage, String.valueOf(minimumSize)));
    }
        
    public String createSetterTemplate(BeanType.Property setter) {
        Member member = setter.getMember();
        if (member instanceof Method) {
            return "#1." + member.getName() + "(#2);";
        } else if (member instanceof Field) {
            return "#1." + member.getName() + " = #2;";
        } else
            throw new IllegalArgumentException("Unrecognized member type: " + member.getClass());
    }
    
    public String createGetterTemplate(BeanType.Property getter) {
        Member member = getter.getMember();
        if (member instanceof Method) {
            return "#1." + member.getName() + "()";
        } else if (member instanceof Field) {
            return "#1." + member.getName();
        } else
            throw new IllegalArgumentException("Unrecognized member type: " + member.getClass());
    }
    
}
