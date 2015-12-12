package go.jyroscope.demo;

import com.jyroscope.annotations.*;

public class DemoSubscriber {
    
    @Subscribe("ros:/string")
    public void receive(StringMessage message) {
        System.out.println("Received: " + message.data);
    }

}
