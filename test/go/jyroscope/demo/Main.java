package go.jyroscope.demo;

import com.jyroscope.*;
import java.nio.file.*;

public class Main {

    public static void main(String[] args) throws CreationException {
        Jyroscope.addMsgSearchPath(Paths.get("msgs").toAbsolutePath().toString());
        Jyroscope jyroscope = new Jyroscope();
        jyroscope.addRemoteMaster("ros", "http://localhost:11311", "localhost", "/jy");

        // Create a new node
        jyroscope.create(DemoSubscriber.class);
        jyroscope.create(DemoPublisher.class);
    }
    
}
