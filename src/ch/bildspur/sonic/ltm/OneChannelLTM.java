package ch.bildspur.sonic.ltm;

import ch.bildspur.sonic.DelayDetector;
import ch.bildspur.sonic.ltm.util.DSP;
import ch.bildspur.sonic.tdao.BaseTDAO;
import ch.bildspur.sonic.util.geometry.Vector2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by cansik on 14/05/16.
 */
public class OneChannelLTM extends BaseTDAO {
    List<LTMPattern> patterns = new ArrayList<>();

    int step = 0;

    @Override
    public Vector2 run() {
        // only toke one data vector
        double[] f = toDA(ll);

        double bestCorrelation = 0;
        LTMPattern bestPattern = new LTMPattern("NO RESULT", Vector2.NULL, null);

        for(LTMPattern p : patterns)
        {
            double[] corrData = DSP.xcorr(f, p.data);
            double val = Arrays.stream(corrData).max().getAsDouble();

            if(val > bestCorrelation)
            {
                bestCorrelation = val;
                bestPattern = p;
            }
        }

        System.out.println("ONELTM: " + bestPattern.name);

        return bestPattern.location;
    }

    public static double[] toDA(float[] arr)
    {
        double[] res =  new double[arr.length];
        for(int i = 0; i < arr.length; i++)
        {
            res[i] = (double)arr[i];
        }
        return res;
    }

    public static float[] toFA(double[] arr)
    {
        float[] res =  new float[arr.length];
        for(int i = 0; i < arr.length; i++)
        {
            res[i] = (float)arr[i];
        }
        return res;
    }

    public void train(String name, Vector2 location, float[] data)
    {
        patterns.add(new LTMPattern(name, location, toDA(data)));
    }

    public void reset()
    {
        patterns.clear();
        step = 0;
    }

    public List<LTMPattern> getPatterns() {
        return patterns;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public void incStep()
    {
        this.step++;
    }
}
