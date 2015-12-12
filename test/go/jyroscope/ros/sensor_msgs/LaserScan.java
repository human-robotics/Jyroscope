package go.jyroscope.ros.sensor_msgs;

import go.jyroscope.ros.std_msgs.Header;
import com.jyroscope.annotations.*;

@Message("sensor_msgs/LaserScan")
public class LaserScan {
    
    public Header header;
    
    @Name("angle_min") public float angleMin;
    @Name("angle_max") public float angleMax;
    @Name("angle_increment") public float angleIncrement;
    @Name("time_increment") public float timeIncrement;
    @Name("scan_time") public float scanTime;
    @Name("range_min") public float rangeMin;
    @Name("range_max") public float rangeMax;
    
    public float[] ranges;
    public float[] intensities;

    @Override
    public String toString() {
        return "LaserScan{" + "header=" + header + ", angleMin=" + angleMin + ", angleMax=" + angleMax + ", angleIncrement=" + angleIncrement + ", timeIncrement=" + timeIncrement + ", scanTime=" + scanTime + ", rangeMin=" + rangeMin + ", rangeMax=" + rangeMax + ", ranges=" + ranges + ", intensities=" + intensities + '}';
    }
    
}
