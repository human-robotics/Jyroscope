package go.jyroscope.ros.geometry_msgs;

import com.jyroscope.annotations.*;
import java.util.*;

@Message("geometry_msgs/TwistWithCovariance")
public class TwistWithCovariance {
    
    public Twist twist;
    public double[] covariance;

    @Override
    public String toString() {
        return "TwistWithCovariance{" + "twist=" + twist + ", covariance=" + Arrays.toString(covariance) + '}';
    }
    
}
