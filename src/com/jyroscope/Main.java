package com.jyroscope;

import com.jyroscope.annotations.*;
import java.nio.file.*;

public class Main {
    
    @Message("std_msgs/String")
    public static class StringMessage {

        private String data;

        public StringMessage() {
            //
        }

        public StringMessage(String data) {
            this.data = data;
        }

        public String getData() {
            return data;
        }

        public void setData(String value) {
            this.data = value;
        }

        @Override
        public String toString() {
            return "StringMessage[" + data + "]";
        }

    }
    
    public static class StringNode {
        @Publish("ros:/string")
        public Publisher<StringMessage> publisher;

        public void publish(String value) {
            StringMessage message = new StringMessage();
            message.setData(value);
            publisher.handle(message);
        }
        
        public void latch(String value) {
            StringMessage message = new StringMessage();
            message.setData(value);
            publisher.latch(message);
        }
        
        @Init
        public void test() {
            publisher.latch(new StringMessage("Hey!"));
        }

        @Subscribe("ros:/string")
        public void addMessage(StringMessage message) {
            System.out.println(message.getData());
        }

        public Publisher<StringMessage> getPublisher() {
            return publisher;
        }
    }
    
    public static void main(String[] args) throws CreationException, InterruptedException {
        Jyroscope.addMsgSearchPath(Paths.get("msgs").toAbsolutePath().toString());
        Jyroscope jyroscope = new Jyroscope();
        jyroscope.addRemoteMaster("ros", "http://localhost:11311", "localhost", "/jy");
        jyroscope.addLocalMaster("local");

        jyroscope.create(StringNode.class);//.latch("Hello, World!");
        
        /*Link<StringMessage> publisher = jyroscope.startNode(StringNode.class).getPublisher();
        int i = 1;
        while (true) {
            Thread.sleep(1000);
            publisher.handle(new StringMessage("Hello, World " + i));
            i++;
        }*/
    }
    
}
