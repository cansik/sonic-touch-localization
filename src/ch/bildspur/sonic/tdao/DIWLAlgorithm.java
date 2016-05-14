package ch.bildspur.sonic.tdao;

import ch.bildspur.sonic.Vector2d;
import ch.bildspur.sonic.util.geometry.Circle;
import ch.bildspur.sonic.util.geometry.Vector2;
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
        runApproximation(channels, 500, 20);

        return new Vector2(0, 0);
    }

    void runApproximation(Channel[] channels, float maxTime, float stepSize)
    {
        // todo: find better cancel criteria (table size)
        float time = stepSize;

        while(time < maxTime)
        {
            // draw circle for debugging
            for(Channel c : channels) {
                Color color = new Color(c.color.getRed(), c.color.getGreen(), c.color.getBlue(), 0.5f);
                drawCircle(c.getCircle(time), color);
            }



            time += stepSize;
        }
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

    public void drawCircle(Circle circle, Color color)
    {
        if(circle == null)
            return;

        Vector2 c = convertToTableSpace(circle.c);
        double r = convertToTableSpace(circle.r);

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setLineWidth(2);
        gc.setStroke(color);
        double hr = r / 2d;
        gc.strokeOval(c.x - hr, c.y - hr, r, r);
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