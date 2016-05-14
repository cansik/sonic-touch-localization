package ch.bildspur.sonic.ltm;

import ch.bildspur.sonic.util.geometry.Vector2;

/**
 * Created by cansik on 14/05/16.
 */
public class LTMPattern {

    public String name;
    public Vector2 location;
    public double[] data;

    public LTMPattern(String name, Vector2 location, double[] data)
    {
        this.name = name;
        this.location = location;
        this.data = data;
    }
}
