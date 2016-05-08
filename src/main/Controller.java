package main;

import ch.bildspur.sonic.*;
import ch.fhnw.ether.audio.JavaSoundSource;
import ch.fhnw.ether.audio.fx.AudioGain;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Slider;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import javax.swing.*;
import java.util.Arrays;

public class Controller implements IGestureHandler {

    @FXML
    public Canvas visCanvasChannel1;

    @FXML
    public Canvas visCanvasChannel2;

    @FXML
    public Canvas visCanvasChannel3;

    @FXML
    public Canvas visCanvasChannel4;

    @FXML
    public Canvas visBufferLeft;

    @FXML
    public Canvas visBufferRight;

    public Circle circleThresholdPassed;
    public Slider sliderThreshold;

    GestureRecognizer gr;

    int bufferSize = 10000;

    LoopRingBuffer bufferLeft = new LoopRingBuffer(bufferSize);
    LoopRingBuffer bufferRight = new LoopRingBuffer(bufferSize);

    boolean thresholdPassed = false;
    int thresholdWait = 10;
    int thresholdTimer = 0;

    public void initialize()
    {
        sliderThreshold.valueProperty().addListener((observable, oldValue, newValue) -> {
            gr.setThreshold(newValue.floatValue());
        });
    }

    public void btnTest_clicked(ActionEvent actionEvent) {
        System.out.println("draw visualisation");
        //drawTableVisualisation();

        String[] sources = AudioUtils.getSources();
        System.out.println(Arrays.toString(sources));

        JavaSoundSource source;

        // 8 channel
        //source = new JavaSoundSource(8, 96000, 256*8);

        // 2 channel
        source = new JavaSoundSource(2, 48000, 256*2);

        LaneRecorder recorder = new LaneRecorder(source, new AudioGain());
        gr = new GestureRecognizer(this);
        recorder.getProgram().addLast(gr);

        sliderThreshold.setValue(gr.getThreshold());

        SwingUtilities.invokeLater(recorder::start);
    }

    void drawBuffer(float[] buffer, Canvas c, Color color, boolean drawThreshold)
    {
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.clearRect(0, 0, c.getWidth(), c.getHeight());
        float space = (float)(c.getWidth() / buffer.length);

        gc.setStroke(Color.BLACK);
        gc.setFill(color);

        gc.strokeRect(1, 1, c.getWidth() - 2, c.getHeight() - 2);

        float y = (float)c.getHeight() / 2f;

        float postAmp = 20; //100000;

        for(int i = 0; i < buffer.length - 1; i++)
        {
            float v = buffer[i];

            gc.fillOval(space * i, y + v * postAmp, 3, 3);
        }

        // draw threshold
        gc.setStroke(Color.GREEN);
        if(drawThreshold) {
            gc.strokeLine(0, y + y * gr.getThreshold() * -1, c.getWidth(), y + y * gr.getThreshold() * -1);
        }
    }

    @Override
    public void bufferReceived(float[][] channels) {
        float[] c1 = Arrays.copyOf(channels[0], channels[0].length);
        float[] c2 = Arrays.copyOf(channels[1], channels[1].length);

        // just some buffer test
        bufferLeft.put(c1);
        bufferRight.put(c2);

        Platform.runLater(() -> drawBuffer(c1, visCanvasChannel1, Color.BLUE, false));
        Platform.runLater(() -> drawBuffer(c2, visCanvasChannel2, Color.RED, false));

        // draw full buffer
        Platform.runLater(() -> drawBuffer(bufferLeft.getBuffer(), visBufferLeft, Color.BLUE, true));
        Platform.runLater(() -> drawBuffer(bufferRight.getBuffer(), visBufferRight, Color.RED, true));


        //threshold
        if(thresholdPassed)
        {
            if(thresholdTimer > thresholdWait)
            {
                circleThresholdPassed.setFill(Color.WHITE);
                thresholdPassed = false;

                // do analyzing
                doAnalyze();
            }
            thresholdTimer++;
        }

        /*
        float[] c3 = Arrays.copyOf(channels[2], channels[2].length);
        float[] c4 = Arrays.copyOf(channels[3], channels[3].length);
        Platform.runLater(() -> drawBuffer(20, 50, c3, visCanvasChannel3));
        Platform.runLater(() -> drawBuffer(20, 50, c4, visCanvasChannel4));
        */

    }

    private void doAnalyze()
    {
        int sampleSize = 300;
        float[] f = bufferLeft.getLatest(sampleSize);
        float[] g = bufferRight.getLatest(sampleSize);

        Anaylizer a = new Anaylizer();
        float corr = a.execCorrelation(f, g);

        System.out.print("Corss: " + corr);

        if(corr > 0)
        {
            System.out.println("\tLEFT");
        }
        else
        {
            System.out.println("\tRIGHT");
        }
    }

    @Override
    public void thresholdPassed() {
        thresholdTimer = 0;
        thresholdPassed = true;
        circleThresholdPassed.setFill(Color.RED);
    }

    public void btnDetect_clicked(ActionEvent actionEvent) {
        LoopRingBuffer lrb1 = new LoopRingBuffer(50000);
        LoopRingBuffer lrb2 = new LoopRingBuffer(50000);

        lrb1.loadBuffer("data/plot1.data");
        lrb2.loadBuffer("data/plot2.data");

        float[] f = lrb1.getBuffer();
        float[] g = lrb2.getBuffer();

        Anaylizer a = new Anaylizer();
        float[] cross = a.crossCorrelation(f, g);

        float corr = a.execCorrelation(f, g);

        /*
        cross = new float[f.length];
        for(int i = 0; i < cross.length; i++)
        {
            cross[i] = a.crossCorrelationBourke(f, g, i, 100);
        }
        */

        LoopRingBuffer lrbC = new LoopRingBuffer(cross.length + 1);
        lrbC.put(cross);
        lrbC.saveBuffer("data/cross.data");

        System.out.println("finished!");

        /*
        float[] cross = new float[f.length];

        for(int n = 0; n < cross.length; n++) {
            for (int m = 0; m < f.length; m++) {
                cross[n] = f[m] * g[m+n];
            }
        }
        */

    }

    public void saveAllBuffer()
    {
        bufferLeft.saveBuffer("data/plot1.data");
        bufferRight.saveBuffer("data/plot2.data");

        System.out.println("buffer saved!");
    }

    public void btnSave_clicked(ActionEvent actionEvent) {
        saveAllBuffer();
    }
}
