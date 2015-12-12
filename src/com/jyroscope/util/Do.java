package com.jyroscope.util;

import com.jyroscope.*;

public class Do {

	public static void later(Runnable runnable) {
		new Thread(runnable).start();
	}
	
	public static void service(Runnable runnable) {
		new Thread(runnable).start();
	}

	public static void connection(Runnable runnable) {
		new Thread(runnable).start();
	}
	
	public static <T> void deliver(T message, Link<T> destination) {
		destination.handle(message);
	}
	
}
