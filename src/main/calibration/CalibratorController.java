package main.calibration;

import ch.bildspur.sonic.util.geometry.Vector2;
import ch.fhnw.util.Pair;
import javafx.animation.KeyValue;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import main.Main;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by cansik on 13/05/16.
 */
public class CalibratorController {

    public Canvas visTable;
    public Map<String, Vector2> edgePoints;

    public Vector2 currentPoint = null;

    public Vector2 getCurrentPoint() {
        return currentPoint;
    }

    public void initialize() {
        Main.calibratorController = this;

        defineEdgePoints();
        clearTable();

        visTable.setFocusTraversable(true);
        visTable.requestFocus();
    }

    public Vector2 convertToVectorSpace(Vector2 v, double vectorWidth, double vectorLength) {
        return new Vector2(v.x / visTable.getWidth() * vectorWidth,
                v.y / visTable.getHeight() * vectorLength);
    }

    public void setupPoint(Vector2 v)
    {
        currentPoint = v;
        double radius = 10;
        double h = radius / 2;
        clearTable();
        GraphicsContext gc = visTable.getGraphicsContext2D();
        gc.setStroke(Color.RED);
        gc.setLineWidth(2);
        gc.strokeOval(v.x - h, v.y - h, radius, radius);
    }

    public void defineEdgePoints()
    {
        // define points
        edgePoints = new HashMap<>();
        edgePoints.put("LL",  new Vector2(0, visTable.getHeight()));
        edgePoints.put("UL",  new Vector2(0, 0));
        edgePoints.put("UR",  new Vector2(visTable.getWidth(), 0));
        edgePoints.put("LR",  new Vector2(visTable.getWidth(), visTable.getHeight()));
    }

    public void clearTable() {
        GraphicsContext gc = visTable.getGraphicsContext2D();
        gc.clearRect(0, 0, visTable.getWidth(), visTable.getWidth());

        // clear with color
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, visTable.getWidth(), visTable.getWidth());

        // draw grid
        gc.setStroke(Color.DARKGRAY);
        gc.strokeLine(visTable.getWidth() / 2, 0, visTable.getWidth() / 2, visTable.getHeight());
        gc.strokeLine(0, visTable.getHeight() / 2, visTable.getWidth(), visTable.getHeight() / 2);

        // draw for each edge point
        for (Map.Entry<String, Vector2> k : edgePoints.entrySet()) {
            Vector2 v = k.getValue();

            // draw edge circles
            double radius = 20;
            double hr = radius / 2;
            gc.setStroke(Color.SPRINGGREEN);
            gc.setLineWidth(2);
            gc.strokeOval(v.x - hr, v.y - hr, radius, radius);

            // draw name
            gc.setLineWidth(1);
            double xSwitch = v.x > 0 ? -1 : 1;
            double ySwitch = v.y > 0 ? -1 : 1;

            gc.strokeText(k.getKey(), v.x + radius * xSwitch, v.y + radius * ySwitch);

            // draw edge points
            radius = 6;
            hr = radius / 2;
            gc.setFill(Color.SPRINGGREEN);
            gc.fillOval(v.x - hr, v.y - hr, radius, radius);
        }
    }

    public void showTimePerSquare(String edgePoint, int widthResolution, int heightResolution) {
        clearTable();
        System.out.println("showing time points for " + edgePoint);

        double widthStep = visTable.getWidth() / (double) widthResolution;
        double heightStep = visTable.getHeight() / (double) heightResolution;

        Vector2 measurePoint = edgePoints.get(edgePoint);

        List<Pair<Vector2, Double>> distances = new ArrayList<>();
        double maxDistance = 0;

        // calculate times
        for (int y = 0; y < heightResolution; y++) {
            for (int x = 0; x < widthResolution; x++) {
                // get point
                double currentX = ((double) x * widthStep) + (widthStep / 2);
                double currentY = ((double) y * heightStep) + (heightStep / 2);

                Vector2 pos = new Vector2(currentX, currentY);

                // calculate time
                double d = pos.distance(measurePoint);
                if (maxDistance < d) maxDistance = d;

                distances.add(new Pair(new Vector2(x, y), d));
            }
        }

        // draw squares
        for (Pair<Vector2, Double> points : distances) {
            Vector2 pos = points.first;
            double distance = points.second;
            double normalizedDistance = distance / maxDistance;

            Color c = new Color(Color.MAGENTA.getRed(), Color.MAGENTA.getGreen(), Color.MAGENTA.getBlue(), normalizedDistance);

            // draw squares
            Platform.runLater(() -> {
                GraphicsContext gc = visTable.getGraphicsContext2D();
                gc.setFill(c);
                gc.fillRect(pos.x * widthStep, pos.y * heightStep, widthStep, heightStep);
            });
        }
    }

    public double getRandomDouble(double min, double max)
    {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    public void OnKeyPressed(KeyEvent event) {
        int widthResolution = 20;
        int heightResolution = 20;

        switch (event.getCode())
        {
            case SPACE:
                System.out.println("RANDOM POINT");
                setupPoint(new Vector2(getRandomDouble(0, visTable.getWidth()), getRandomDouble(0, visTable.getHeight())));
                break;
            case A:
                System.out.println("A");
                break;
            case DIGIT1:
                showTimePerSquare("LL", widthResolution, heightResolution);
                break;
            case DIGIT2:
                showTimePerSquare("UL", widthResolution, heightResolution);
                break;
            case DIGIT3:
                showTimePerSquare("UR", widthResolution, heightResolution);
                break;
            case DIGIT4:
                showTimePerSquare("LR", widthResolution, heightResolution);
                break;
        }
    }
}
