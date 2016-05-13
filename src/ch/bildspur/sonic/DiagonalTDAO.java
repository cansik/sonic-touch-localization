package ch.bildspur.sonic;

import ch.bildspur.sonic.util.geometry.Line2;
import ch.bildspur.sonic.util.geometry.LineIntersection;
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

    public Vector2 run()
    {
        // define positions by table size
        posLL = new Vector2(0, tableWidth);
        posUL = new Vector2(0, 0);
        posUR = new Vector2(tableLength, 0);
        posLR = new Vector2(tableLength, tableWidth);

        drawDiagonals();

        // calculate distance point of e
        Vector2 E = getDistancePoint(ll, ur, posLL, posUR);

        // calculate distnace point of f
        Vector2 F = getDistancePoint(ul, lr, posUL, posLR);

        // draw vectors
        drawVector(E, "E", Color.BLUE);
        drawVector(F, "F", Color.BLUE);

        // calculate orthogonal vector to e
        Vector2 orthE = getOrthogonalVector(posLL, posUR, E, 0.1);

        // calculate orthogonal vector to f
        Vector2 orthF = getOrthogonalVector(posUL, posLR, F, 0.1);

        drawVector(orthE, "oE", Color.GREEN);
        drawVector(orthF, "oF", Color.GREEN);

        // intersect v2 and v2
        Line2 g = new Line2(E, orthE);
        Line2 h = new Line2(F, orthF);

        LineIntersection result = g.intersect(h);

        drawLine(result.s, E, Color.CYAN);
        drawLine(result.s, F, Color.CYAN);
        drawVector(result.s, "X", Color.RED);

        return result.s;
    }


    Vector2 getOrthogonalVector(Vector2 A, Vector2 B, Vector2 OP, double length)
    {
        Vector2 AB = B.sub(A);
        Vector2 ABorth = AB.rotPlus90().normalize(); //new Vector2(AB.y, -AB.x).normalize();
        Vector2 Q = ABorth.scale(length).add(OP);
        return Q;
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

    void drawDiagonals()
    {
        drawLine(posLL, posUR, Color.ORANGE);
        drawLine(posUL, posLR, Color.ORANGE);
    }

    void drawLine(Vector2 a, Vector2 b, Color c)
    {
        Vector2 cA = convertToTableSpace(a);
        Vector2 cB = convertToTableSpace(b);

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setStroke(c);
        gc.strokeLine(cA.x, cA.y, cB.x, cB.y);
    }

    void drawVector(Vector2 v, String name, Color c)
    {
        float size = 5;
        float hs = size / 2;
        Vector2 vC = convertToTableSpace(v);
        // canvas for drawing
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setStroke(c);
        gc.strokeOval(vC.x - hs, vC.y - hs, size, size);
        gc.strokeText(name, vC.x + size, vC.y + size);
    }
}
