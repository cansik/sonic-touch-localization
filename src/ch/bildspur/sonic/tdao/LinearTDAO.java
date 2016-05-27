package ch.bildspur.sonic.tdao;

import ch.bildspur.sonic.util.geometry.Vector2;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import main.analyzer.AnalyzerController;
import main.analyzer.Function2;

/**
 * Created by cansik on 13/05/16.
 */
public class LinearTDAO extends BaseTDAO {

    public Vector2 run() {
        float[] f = ll;
        float[] g = ul;
        float[] h = ur;
        float[] k = lr;

        // prepare params
        float sonicSpeed = AnalyzerController.SONIC_SPEED; // m/s
        float samplingRate = AnalyzerController.SAMPLING_RATE; // hz (iphone: 44100)

        float tableDiag = (float)Math.sqrt(Math.pow(tableLength, 2) + Math.pow(tableWidth, 2)); // m (iphone sqrt(5))

        // calculate path percentage
        double leftPer = getPercentagePosition(sonicSpeed, samplingRate, (float)tableWidth, f, g, delayAlgorithm);
        double rightPer = getPercentagePosition(sonicSpeed, samplingRate, (float)tableWidth, k, h, delayAlgorithm);
        double topPer = getPercentagePosition(sonicSpeed, samplingRate, (float)tableLength, g, h, delayAlgorithm);
        double bottomPer = getPercentagePosition(sonicSpeed, samplingRate, (float)tableLength, f, k, delayAlgorithm);

        double diagnoal1 = getPercentagePosition(sonicSpeed, samplingRate, tableDiag, f, h, delayAlgorithm);
        double diagnoal2 = getPercentagePosition(sonicSpeed, samplingRate, tableDiag, g, k, delayAlgorithm);

        // draw result
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // draw lines
        double width = canvas.getWidth();
        double height = canvas.getHeight();

        double size = 10;
        double hs = size / 2;

        // draw grid
        gc.setStroke(Color.DARKGRAY);
        gc.strokeLine(width / 2, 0, width / 2, height);
        gc.strokeLine(0, height / 2, width, height / 2);

        // left + top
        gc.setStroke(Color.BLUE);
        gc.strokeOval(width * topPer - hs, height * leftPer - hs, size, size);

        // left + bottom
        gc.setStroke(Color.RED);
        gc.strokeOval(width * bottomPer - hs, height * leftPer - hs, size, size);

        // right + top
        gc.setStroke(Color.CYAN);
        gc.strokeOval(width * topPer - hs, height * rightPer - hs, size, size);

        // right + bottom
        gc.setStroke(Color.ORANGE);
        gc.strokeOval(width * bottomPer - hs, height * rightPer - hs, size, size);

        // diagonal 1
        gc.setStroke(Color.MAGENTA);
        gc.strokeOval(width * diagnoal1 - hs, height * diagnoal1 - hs, size, size);

        // diagonal 2
        gc.setStroke(Color.LIMEGREEN);
        gc.strokeOval(width * diagnoal2 - hs, height * diagnoal2 - hs, size, size);

        // calculate center point
        double meanX = (width * topPer + width * bottomPer) / 2; //+ width * diagnoal1 + width * diagnoal2) / 4;
        double meanY = (height * rightPer + height * leftPer) / 2; // + height * diagnoal1 + height * diagnoal2) / 4;

        double undMeanX = (tableLength * topPer + tableLength * bottomPer) / 2;
        double undMeanY =  (tableWidth * rightPer + tableWidth * leftPer) / 2;

        controller.lastPoint = new Vector2(undMeanX, undMeanY);
        controller.log("P: (" + undMeanX + "|" + undMeanY + ")");

        // draw arrow
        gc.setStroke(Color.BLUE);
        gc.strokeLine(meanX, meanY, width / 2, height / 2);

        // draw center
        gc.setStroke(Color.GOLD);
        gc.strokeOval(meanX - hs, meanY - hs, size, size);

        // draw border
        gc.setStroke(Color.BLACK);
        gc.strokeRect(1, 1, width - 2, height - 2);

        return new Vector2(meanX, meanY);
    }

    double getPercentagePosition(float sonicSpeed, float samplingRate, float tableLength, float[] f, float[] g, Function2<float[], float[], Float> algorithm) {
        double delta = algorithm.apply(f, g);
        double fullTime = 1d / sonicSpeed * tableLength;
        double samplesForDistance = fullTime * samplingRate;
        double sampleWay = (samplesForDistance / 2d) + (delta / 2d);

        System.out.print("Table length (m): " + tableLength);
        System.out.print("\tDelta (smp): " + delta);
        System.out.print("\tFullTime (s): " + fullTime);
        System.out.print("\tSamples Full (smp): " + samplesForDistance);
        System.out.print("\tSamples Way (smp): " + sampleWay);
        System.out.println();

        return (sampleWay / samplesForDistance);
    }
}
