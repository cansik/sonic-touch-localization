package ch.bildspur.sonic.tdao;

import ch.bildspur.sonic.Vector2d;
import ch.bildspur.sonic.util.geometry.Circle;
import ch.bildspur.sonic.util.geometry.CircleCircleIntersection;
import ch.bildspur.sonic.util.geometry.Vector2;
import ch.fhnw.util.DoubleList;
import ch.fhnw.util.Pair;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import main.analyzer.AnalyzerController;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by cansik on 12/05/16.
 */
public class DIWLAlgorithm extends BaseTDAO {

    public Vector2 run()
    {
        calculateMicPositions();

        Channel[] channels = new Channel[]{
                new Channel("LL", posLL, ll, Color.BLUE),
                new Channel("UL", posUL, ul, Color.RED),
                new Channel("UR", posUR, ur, Color.GREEN),
                new Channel("LR", posLR, lr, Color.ORANGE)
        };

        // detect signal income order
        // calculate sample difference between 0-1, 1-2, 2-3 ...
        // result: 1->15->2->23->3 ...
        orderByIncoming(channels);

        // create circle with radius from pos to 1 (2 -> 15), (3 -> 38) ...

        // draw initial circle
        for(Channel c : channels)
                drawCircle(c.getCircle(0), c.color);

        // with sample size, calculate all circles around one (0 - intersect with diagonal position)
        // get the shortest intersection points of all circles (on table)
        // calculate emphasis of intersection points
        // finally we have a position
        Vector2 centroid = runApproximation(channels, 0.01f);
        drawCross(centroid, Color.GREEN);

        // print result
        controller.lastPoint = centroid;
        controller.log("P: (" + centroid.x + "|" + centroid.y + ")");

        return convertToTableSpace(centroid);
    }

    void circleTest() {
        // test
        double htL = tableLength / 2d;
        double htW = tableWidth / 2d;
        Circle c1 = new Circle(new Vector2(htL - (htL * 0.25), htW), htW * 1.5 / 2d);
        Circle c2 = new Circle(new Vector2(htL + (htL * 0.25), htW), htW * 1.5 / 2d);

        drawCircle(c1, Color.RED);
        drawCircle(c2, Color.BLUE);

        CircleCircleIntersection intersection = new CircleCircleIntersection(c1, c2);
        for (Vector2 v : intersection.getIntersectionPoints())
            drawCross(new Vector2(v.x, v.y), Color.GREEN);
    }

    Vector2 runApproximation(Channel[] channels, float stepSize)
    {
        // todo: find better cancel criteria (table size)
        float time = stepSize;

        // best values
        double minDistances = Double.MAX_VALUE;
        Vector2 minCentroidPoint = Vector2.NULL;

        // gauss
        int n = channels.length - 1;
        int intersectionCount = (int) ((Math.pow(n, 2) + n) / 2);
        CircleCircleIntersection[] intersections = new CircleCircleIntersection[intersectionCount];

        int iterationCount = 0;

        // cancel criteria
        double maxRadius = Math.sqrt(Math.pow(tableLength, 2) + Math.pow(tableWidth, 2));
        boolean running = true;

        while (running)
        {
            iterationCount++;
            // draw circle for debugging
            for(Channel c : channels) {
                Color color = new Color(c.color.getRed(), c.color.getGreen(), c.color.getBlue(), 0.5f);
                //drawCircle(c.getCircle(time), color);
            }

            boolean circlesDisjunct = false;

            // calculate intersections
            int intersectionCounter = 0;
            for (int i = 0; i < n; i++) {
                Channel c1 = channels[i];
                for (int j = i + 1; j < channels.length; j++) {
                    Channel c2 = channels[j];
                    //System.out.println("Intersect: " + c1.name + " with " + c2.name);

                    Circle circle1 = c1.getCircle(time);
                    Circle circle2 = c2.getCircle(time);
                    CircleCircleIntersection intersection = new CircleCircleIntersection(circle1, circle2);

                    // check if iteration can stop
                    if (circle1.r > maxRadius || circle2.r > maxRadius)
                        running = false;

                    int relevantIntersections = 0;
                    for (Vector2 v : intersection.getIntersectionPoints())
                        if (isVectorOnTable(v))
                            relevantIntersections++;

                    if (relevantIntersections == 0)
                        circlesDisjunct = true;

                    intersections[intersectionCounter++] = intersection;
                }
            }

            // update time
            time += stepSize;

            // continue only if all circles have an intersection with each other
            if (circlesDisjunct)
                continue;

            // draw relevant intersection points
            for (CircleCircleIntersection intersection : intersections) {
                for (Vector2 v : intersection.getIntersectionPoints()) {
                    drawCross(v, Color.RED);
                }
            }

            // calculate best centroid Pair<Distance, Point>
            Pair<Double, Vector2> result = getCentroid(intersections);

            if (minDistances > result.first) {
                minDistances = result.first;
                minCentroidPoint = result.second;
            }
        }

        System.out.println("Iteration: " + iterationCount + "\t | ");

        return minCentroidPoint;
    }

    Pair<Double, Vector2> getCentroid(CircleCircleIntersection[] intersections) {
        // todo: cleanup this code!

        // calculate big centroid
        Vector2 fullCentroid = Vector2.NULL;
        int fullCount = 0;
        for (CircleCircleIntersection intersection : intersections) {
            for (Vector2 v : intersection.getIntersectionPoints()) {
                fullCentroid = fullCentroid.add(v);
                fullCount++;
            }
        }
        fullCentroid = fullCentroid.scale(1d / (double) fullCount);
        drawCross(fullCentroid, Color.BLUE);

        // foreach intersection take nearest intersection point
        Vector2[] nearest = new Vector2[intersections.length];
        for (int i = 0; i < intersections.length; i++) {
            Vector2[] points = intersections[i].getIntersectionPoints();

            // if tangent
            if (points.length == 1) {
                nearest[i] = points[0];
                continue;
            }

            // if two intersections
            double d1 = fullCentroid.distance(points[0]);
            double d2 = fullCentroid.distance(points[1]);

            if (d1 < d2)
                nearest[i] = points[0];
            else
                nearest[i] = points[1];
        }

        // draw points for debugging
        for (Vector2 v : nearest)
            drawCross(v, Color.CYAN);

        // calculate centroid for all relevant and nearest points
        Vector2 centroid = Vector2.NULL;
        for (Vector2 v : nearest)
            centroid = centroid.add(v);
        centroid = centroid.scale(1d / (double) nearest.length);

        // calculate distance for each point to centroid and sum up (fitness)
        double fitness = 0;
        for (Vector2 v : nearest)
            fitness += centroid.distance(v);

        System.out.println("Fitness: " + fitness + " | " + centroid.toString());

        return new Pair<>(fitness, centroid);
    }

    boolean isVectorOnTable(Vector2 v) {
        return (v.x >= 0 && v.x < tableLength) && (v.y >= 0 && v.y < tableWidth);
    }

    /**
     * Returns distance in m per time in samples.
     * @param time
     * @return Distance for time.
     */
    public static double getDistanceByTime(float time)
    {
        double oneSampleDistance =  AnalyzerController.SONIC_SPEED / AnalyzerController.SAMPLING_RATE;
        return oneSampleDistance * (double)time;
    }

    public void orderByIncoming(Channel[] channels)
    {
        Channel ref = channels[0];

        // calculate distance to reference
        for(int i = 1; i < channels.length; i++)
        {
            Channel c = channels[i];

            // calculate difference between channels
            // use distance as distance to reference
            c.distance = delayAlgorithm.apply(ref.data, c.data);
        }

        // sort by distance (descending)
        Arrays.sort(channels, (o1, o2) -> Float.compare(o2.distance, o1.distance));

        // add distance (so first channel has 0 distance)
        float distance = channels[0].distance;
        channels[0].distance = 0;

        for(int i = 1; i < channels.length; i++)
        {
            // subtract distance and invert time
            channels[i].distance -= distance;
            channels[i].distance *= -1;
        }
    }

    public void drawCross(Vector2 v, Color color) {
        drawCross(v, color, 3);
    }

    public void drawCross(Vector2 vector, Color color, double size) {
        double hs = size / 2d;
        GraphicsContext gc = canvas.getGraphicsContext2D();
        Vector2 v = convertToTableSpace(vector);
        gc.setStroke(color);
        gc.setFill(color);
        gc.strokeLine(v.x - hs, v.y - hs, v.x + hs, v.y + hs);
        gc.strokeLine(v.x - hs, v.y + hs, v.x + hs, v.y - hs);
    }

    public void drawCircle(Circle circle, Color color)
    {
        if(circle == null)
            return;

        Vector2 c = convertToTableSpace(circle.c);
        double d = convertToTableSpace(circle.r) * 2d;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setLineWidth(1);
        gc.setStroke(color);
        double r = d / 2d;
        gc.strokeOval(c.x - r, c.y - r, d, d);
    }

    public static int getPeekPosition(float[] f)
    {
        int maxPosF = -1;
        float maxValueF = 0;

        for(int i = 0; i < f.length; i++)
        {
            float absF = Math.abs(f[i]);
            if(absF > maxValueF)
            {
                maxValueF = absF;
                maxPosF = i;
            }
        }

        return maxPosF;
    }

    Vector2 convertToTableSpace(Vector2 v)
    {
        return new Vector2((v.x / tableLength) * canvas.getWidth(), (v.y / tableWidth) * canvas.getHeight());
    }

    double convertToTableSpace(double v)
    {
        return (v / tableLength) * canvas.getWidth();
    }
}