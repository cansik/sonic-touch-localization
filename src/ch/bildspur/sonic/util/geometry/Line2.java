package ch.bildspur.sonic.util.geometry;

/**
 * Created by cansik on 13/05/16.
 */
public final class Line2 {
    Vector2 A;
    Vector2 R;

    public Line2(Vector2 A, Vector2 B)
    {
        this.A = A;
        this.R = B.sub(A);
    }

    public LineIntersection intersect(Line2 b)
    {
        Line2 a = this;

        //g
        Vector2 oa = a.A;
        Vector2 ab = a.R;

        //h
        Vector2 oc = b.A;
        Vector2 cd = b.R;

        //create linear system
        //lambda
        Vector2 l = new Vector2 (ab.x, ab.y);

        //my
        Vector2 m = new Vector2 (0 - cd.x, 0 - cd.y);

        //num
        Vector2 n = new Vector2 (oc.x - oa.x, oc.y - oa.y);

        //solve
        //if det = 0 => both vectors are over each other
        double det = (l.x * m.y) - (m.x * l.y);
        double x1 = (n.x * m.y - (m.x * n.y)) / det;
        double x2 = (l.x * n.y - (n.x * l.y)) / det;

        Vector2 s = oc.add (cd.scale (x2));

        return new LineIntersection (s, x1, x2);
    }
}
