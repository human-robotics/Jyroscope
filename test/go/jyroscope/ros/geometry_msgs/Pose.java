package go.jyroscope.ros.geometry_msgs;

import com.jyroscope.annotations.*;

@Message("geometry_msgs/Pose")
public class Pose {

    public Point position;
    public Quaternion orientation;

    @Override
    public String toString() {
        return "Pose{" + "position=" + position + ", orientation=" + orientation + '}';
    }
    
}
