package go.jyroscope.demo;

import com.jyroscope.*;
import com.jyroscope.annotations.*;

public class DemoPublisher {
    
    @Publish("ros:/string")
    public Publisher<StringMessage> publisher;

    @Repeat(interval = 2000)
    public void sendMessage() {
        StringMessage message = new StringMessage();
        message.data = "Hello, World!";
        System.out.println("Sending: " + message.data);
        publisher.handle(message);
    }
    
}