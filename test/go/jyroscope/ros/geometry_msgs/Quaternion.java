package go.jyroscope.ros.geometry_msgs;

import com.jyroscope.annotations.*;

@Message("geometry_msgs/Quaternion")
public class Quaternion {
    
    public double x;
    public double y;
    public double z;
    public double w;

    @Override
    public String toString() {
        return "Quaternion{" + "x=" + x + ", y=" + y + ", z=" + z + ", w=" + w + '}';
    }
    
}
