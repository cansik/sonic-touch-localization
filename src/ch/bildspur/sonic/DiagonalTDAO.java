package ch.bildspur.sonic;

import ch.bildspur.sonic.util.geometry.Vector2;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import main.analyzer.Function2;

/**
 * Created by cansik on 13/05/16.
 */
public class DiagonalTDAO {

    public float[] ll;
    public float[] ul;
    public float[] ur;
    public float[] lr;

    public Function2<float[], float[], Float> delayAlgorithm;

    public double tableLength;
    public double tableWidth;

    public Canvas canvas;

    private Vector2 posLL;
    private Vector2 posUL;
    private Vector2 posUR;
    private Vector2 posLR;

    public void run()
    {
        // canvas for drawing
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // define positions by table size
        posLL = new Vector2(0, tableWidth);
        posUL = new Vector2(0, 0);
        posUR = new Vector2(tableLength, 0);
        posLR = new Vector2(tableLength, tableWidth);

        // calculate distance point of e
        Vector2 E = getDistancePoint(ll, ur, posLL, posUR);

        // calculate distnace point of f
        Vector2 F = getDistancePoint(ul, lr, posUL, posLR);

        // draw vectors
        Vector2 eC = convertToTableSpace(E);
        Vector2 fC = convertToTableSpace(F);

        gc.setStroke(Color.BLUE);
        gc.strokeOval(eC.x, eC.y, 10, 10);
        gc.strokeOval(fC.x, fC.y, 10, 10);

        // calculate diagonal vector to e
        // calculate diagnonal vector to f

        // intersect v2 and v2
    }

    Vector2 convertToTableSpace(Vector2 v)
    {
        return new Vector2((v.x / tableLength) * canvas.getWidth(), (v.y / tableWidth) * canvas.getHeight());
    }

    Vector2 getDistancePoint(float[] f, float[] g, Vector2 fPos, Vector2 gPos)
    {
        Vector2 v = gPos.sub(fPos);
        double distance = Math.abs(Math.sqrt(Math.pow(v.x, 2) + Math.pow(v.y, 2)));

        double delta = delayAlgorithm.apply(f, g);
        double fullTime = 1 / DIWLAlgorithm.SONIC_SPEED * distance;
        double samplesForDistance = fullTime * DIWLAlgorithm.SAMPLING_RATE;
        double sampleWay = (samplesForDistance / 2) + delta;

        double percentagePosition = (sampleWay / samplesForDistance);

        System.out.print("\tDelta (smp): " + delta);
        System.out.print("\tFullTime (s): " + fullTime);
        System.out.print("\tSamples Full (smp): " + samplesForDistance);
        System.out.print("\tSamples Way (smp): " + sampleWay);
        System.out.print("\tPercentage (%): " + percentagePosition);
        System.out.println();

        // get point
        Vector2 vScaled = v.scale(percentagePosition);
        Vector2 signalStart = fPos.add(vScaled);

        return signalStart;
    }
}
