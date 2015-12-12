package go.jyroscope.ros.std_msgs;

import com.jyroscope.annotations.*;
import java.time.*;

@Message("std_msgs/Header")
public class Header {
    
    public int seq;
    public Instant stamp;
    @Name("frame_id")
    public String frameId;

    @Override
    public String toString() {
        return "Header{" + "seq=" + seq + ", stamp=" + stamp + ", frameId=" + frameId + '}';
    }
    
}
