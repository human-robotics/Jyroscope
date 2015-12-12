package go.jyroscope.ros.geometry_msgs;

import com.jyroscope.annotations.*;

@Message("geometry_msgs/Point")
public class Point {
    
    public double x;
    public double y;
    public double z;

    @Override
    public String toString() {
        return "Point{" + "x=" + x + ", y=" + y + ", z=" + z + '}';
    }
    
}
