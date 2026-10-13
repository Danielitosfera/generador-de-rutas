package trafiqueishon.geo;

import java.util.List;

public class Via {
    public String id;
    public String highway;
    public String name;
    public int lanes;
    public boolean oneway;
    public boolean reverseOneway;
    public int maxspeed;
    public String surface;
    public boolean bridge;
    public String ref;

    public double[] projX;
    public double[] projY;

    public double minX, minY, maxX, maxY;

    public transient List<double[]> coords;

    public Via() {}

    public void liberarCoords() {
        if (coords != null) {
            coords.clear();
            coords = null;
        }
    }

    @Override
    public String toString() {
        return "Via{" + id + ", " + highway + ", " + name + "}";
    }
}