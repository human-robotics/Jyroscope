package com.jyroscope.annotations;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Repeat {

    public int delay() default 0;
    public int interval() default 0;
    public int count() default 0;
    
}
