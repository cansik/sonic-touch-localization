package ch.bildspur.sonic;

/**
 * Created by cansik on 11/05/16.
 */
public class Vector2d {

    protected double x = 0;
    protected double y = 0;

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public Vector2d()
    {
    }

    public Vector2d(double x, double y)
    {
        this.x = x;
        this.y = y;
    }

    public Vector2d(Vector2d v)
    {
        this.x = v.x;
        this.y = v.y;
    }

    public Vector2d add(Vector2d v)
    {
        return new Vector2d(x + v.x, y + v.y);
    }

    public Vector2d sub(Vector2d v)
    {
        return new Vector2d(x - v.x, y - v.y);
    }

    public double distance(Vector2d v)
    {
        Vector2d o = sub(v);
        return Math.abs(Math.sqrt(Math.pow(o.x, 2) + Math.pow(o.y, 2)));
    }
}
