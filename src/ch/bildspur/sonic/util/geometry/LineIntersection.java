package ch.bildspur.sonic.util.geometry;

/**
 * Created by cansik on 13/05/16.
 */
public class LineIntersection {
        public Vector2 s;
        public double lambda;
        public double my;

    public boolean IsSpecialIntersection ()
    {
        if (my < 0) {
            my = 10;
        }

        if (lambda < 0) {
            lambda = 10;
        }

        return my < 1 || lambda < 1;
    }

    public LineIntersection (Vector2 s, double l, double m)
    {
        this.s = s;
        this.lambda = l;
        this.my = m;
    }
}
