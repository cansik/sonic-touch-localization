package ch.bildspur.sonic.tdao;

import ch.bildspur.sonic.util.geometry.Circle;
import ch.bildspur.sonic.util.geometry.Vector2;
import javafx.scene.paint.Color;

/**
 * Created by cansik on 14/05/16.
 */
public class Channel {
    protected String name;
    protected float[] data;
    protected float distance = 0; // samples
    protected Vector2 position;
    protected double radius = 0;
    protected Color color;

    public Channel(String name, Vector2 position, float[] data, Color color)
    {
        this.name = name;
        this.data = data;
        this.position = position;
        this.color = color;
    }

    public Channel(String name, Vector2 position, float[] data)
    {
        this(name, position, data, Color.BLACK);
    }

    public Circle getCircle(float time)
    {
        float fullTime = time + this.distance;
        double radius = DIWLAlgorithm.getDistanceByTime(fullTime);
        return radius > 0 ? new Circle(position, radius) : null;
    }
}
