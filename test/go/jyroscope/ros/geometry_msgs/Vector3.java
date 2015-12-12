package go.jyroscope.ros.geometry_msgs;

import com.jyroscope.annotations.*;

@Message("geometry_msgs/Vector3")
public class Vector3 {
    
    public double x;
    public double y;
    public double z;

    public Vector3() {
        // no-arg constructor
    }

    public Vector3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public String toString() {
        return "Vector3{" + "x=" + x + ", y=" + y + ", z=" + z + '}';
    }
    
}
