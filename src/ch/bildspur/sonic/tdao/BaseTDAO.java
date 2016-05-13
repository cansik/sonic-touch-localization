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
}
