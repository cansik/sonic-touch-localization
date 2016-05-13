package ch.bildspur.sonic;

import main.analyzer.Function2;

/**
 * Created by cansik on 12/05/16.
 */
public class DIWLAlgorithm {
    public static float SONIC_SPEED = 343.2f; // m/s
    public static float SAMPLING_RATE = 96000; // hz

    Function2<float[], float[], Double> distanceMethod;

    public DIWLAlgorithm()
    {
    }

    int getPeekPosition(float[] f)
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

    Vector2d run()
    {
        // detect signal income order
        // calculate sample difference between 0-1, 1-2, 2-3 ...
        // result: 1->15->2->23->3 ...
        // create circle with radius from pos to 1 (2 -> 15), (3 -> 38) ...
        // with sample size, calculate all circles around one (0 - intersect with diagonal position)
        // get the shortest intersection points of all circles (on table)
        // calculate emphasis of intersection points
        // finally we have a position

        return new Vector2d();
    }
}