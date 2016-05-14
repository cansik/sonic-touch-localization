package ch.bildspur.sonic.tdao;

import ch.bildspur.sonic.util.geometry.Vector2;
import javafx.scene.canvas.Canvas;
import main.analyzer.AnalyzerController;
import main.analyzer.Function2;

/**
 * Created by cansik on 13/05/16.
 */
public abstract class BaseTDAO {
    public float[] ll;
    public float[] ul;
    public float[] ur;
    public float[] lr;

    public Function2<float[], float[], Float> delayAlgorithm;

    public double tableLength;
    public double tableWidth;

    public Canvas canvas;
    public AnalyzerController controller;

    public abstract Vector2 run();

    protected Vector2 posLL;
    protected Vector2 posUL;
    protected Vector2 posUR;
    protected Vector2 posLR;

    public void calculateMicPositions()
    {
        // define positions by table size
        posLL = new Vector2(0, tableWidth);
        posUL = new Vector2(0, 0);
        posUR = new Vector2(tableLength, 0);
        posLR = new Vector2(tableLength, tableWidth);
    }
}
