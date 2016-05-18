package main;

import ch.bildspur.sonic.*;
import ch.fhnw.ether.audio.JavaSoundSource;
import ch.fhnw.ether.audio.fx.AudioGain;
import ch.fhnw.ether.audio.fx.DCRemove;
import ch.fhnw.ether.audio.fx.LowPass;
import ch.fhnw.ether.audio.fx.OnsetDetect;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import main.analyzer.AnalyzerController;

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

    public Canvas visBufferLL;
    public Canvas visBufferLU;
    public Canvas visBufferRU;
    public Canvas visBufferRL;

    public Circle circleThresholdPassed;
    public Slider sliderThreshold;
    public Canvas visAnalyzing;
    public Canvas visAnalyzingRight;
    public Slider sliderAmp;
    public Canvas visTable;
    public Canvas visLevels;
    public CheckBox cbSendToAnalyzer;
    public TextField tbThresholdValue;
    public TextField tbGainValue;

    JavaSoundSource source;

    GestureRecognizer gr;
    Levels levels;
    AudioGain gain;

    int bufferSize = 30000;
    int thresholdSampleSize = 10000;

    LoopRingBuffer bufferLL = new LoopRingBuffer(bufferSize);
    LoopRingBuffer bufferLU = new LoopRingBuffer(bufferSize);
    LoopRingBuffer bufferRU = new LoopRingBuffer(bufferSize);
    LoopRingBuffer bufferRL = new LoopRingBuffer(bufferSize);

    boolean thresholdPassed = false;
    int thresholdWait = 1;
    int thresholdTimer = 0;

    float[] f;
    float[] g;

    LaneRecorder recorder;

    AnalyzerController analyzerController;

    public void initialize()
    {
        Main.inputController = this;

        sliderThreshold.valueProperty().addListener((observable, oldValue, newValue) -> {
            gr.setThreshold(newValue.floatValue());
            tbThresholdValue.setText(String.format("%f", newValue.floatValue()));
        });

        sliderAmp.valueProperty().addListener((observable, oldValue, newValue) -> {
            setGain(newValue.floatValue());
        });
    }

    public GestureRecognizer getGestureRecognizer() {
        return gr;
    }

    public void setGain(float value)
    {
        gain.setVal("gain", value);
        gr.setGain(value);
        tbGainValue.setText(String.format("%f", value));
    }

    public void btnTest_clicked(ActionEvent actionEvent) {
        System.out.println("draw visualisation");
        //drawTableVisualisation();

        analyzerController = Main.analyzeController;

        String[] sources = AudioUtils.getSources();
        System.out.println(Arrays.toString(sources));

        // 8 channel
        source = new JavaSoundSource(8, 96000, 256 * 8 * 10);

        // 2 channel
        //source = new JavaSoundSource(2, 96000, 256);

        levels = new Levels();
        gain = new AudioGain();

        recorder = new LaneRecorder(source, new DCRemove(), gain, new LowPass(0), levels);
        gr = new GestureRecognizer(this);
        recorder.getProgram().addLast(gr);

        sliderThreshold.setValue(gr.getThreshold());

        Thread t = new Thread(recorder::start);
        t.setDaemon(true);
        t.start();

        //SwingUtilities.invokeLater(recorder::start);
    }

    void drawTableResult(int thresholdResult, float crossBourke)
    {
        GraphicsContext gc = visTable.getGraphicsContext2D();
        gc.clearRect(0, 0, visTable.getWidth(), visTable.getHeight());

        double centerWidth = visTable.getWidth() / 2;

        // draw threshold
        if(thresholdResult == 1)
        {
            gc.setFill(Color.CYAN);
            gc.fillRect(0, 0, centerWidth, visTable.getHeight());
        }
        else if(thresholdResult == -1)
        {
            gc.setFill(Color.MAGENTA);
            gc.fillRect(centerWidth, 0, centerWidth, visTable.getHeight());
        }
        else
        {
            gc.setFill(Color.DARKGRAY);
            gc.fillRect(centerWidth - 10, 0, 20, visTable.getHeight());
        }

        // draw bourke
        gc.setFill(Color.BLACK);
        gc.fillOval(visTable.getWidth() * crossBourke, visTable.getHeight() / 2, 10, 10);

        // border
        gc.setStroke(Color.BLACK);
        gc.strokeRect(1, 1, visTable.getWidth() - 2, visTable.getHeight() - 2);
    }

    void drawBuffer(float[] buffer, Canvas c, Color color, boolean drawThreshold, int pos)
    {
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.clearRect(0, 0, c.getWidth(), c.getHeight());
        float space = (float)(c.getWidth() / buffer.length);

        gc.setFill(color);

        float y = (float)c.getHeight() / 2f;

        for(int i = 0; i < buffer.length - 1; i++)
        {
            float v = buffer[i];

            gc.fillOval(space * i, y + (y * v), 3, 3);
        }

        // draw threshold
        if(drawThreshold) {
            gc.setStroke(Color.GREEN);
            gc.strokeLine(0, y + y * gr.getThreshold() * -1, c.getWidth(), y + y * gr.getThreshold() * -1);
        }

        //draw position
        if(pos >= 0)
        {
            gc.setStroke(Color.GRAY);
            gc.strokeLine(space * pos, 0, space * pos, c.getHeight());
        }

        // draw border
        gc.setStroke(Color.BLACK);
        gc.strokeRect(1, 1, c.getWidth() - 2, c.getHeight() - 2);
    }

    void drawLevels()
    {
        GraphicsContext gc = visLevels.getGraphicsContext2D();
        gc.clearRect(0, 0, visLevels.getWidth(), visLevels.getHeight());

        int channelCount = source.getNumChannels();
        double cWidth = visLevels.getWidth() / channelCount;

        for(int i = 0; i < channelCount; i++)
        {
            float l = levels.getLevel(i);
            gc.setFill(Color.GOLD);
            gc.fillRect(i * cWidth, (1f-l) * visLevels.getHeight(), cWidth, l * visLevels.getHeight());
        }

        // draw threshold
        gc.setStroke(Color.GREEN);
        float t = gr.getThreshold();
        double y = (1f-t) * visLevels.getHeight();
        gc.strokeLine(0, y, visLevels.getWidth(), y);

        // draw border
        gc.setStroke(Color.BLACK);
        gc.strokeRect(1, 1, visLevels.getWidth() - 2, visLevels.getHeight() - 2);
    }

    @Override
    public void bufferReceived(float[][] channels) {
        //float[] c1 = Arrays.copyOf(channels[0], channels[0].length);
        //float[] c2 = Arrays.copyOf(channels[1], channels[1].length);

        float[] cLL = channels[0];
        float[] cLU = channels[1];
        float[] cRU = channels[2];
        float[] cRL = channels[3];

        // add samples to long buffer
        bufferLL.put(cLL);
        bufferLU.put(cLU);
        bufferRU.put(cRU);
        bufferRL.put(cRL);

        // draw current channel
        Platform.runLater(() -> drawBuffer(cLL, visCanvasChannel1, Color.BLUE, false, -1));
        Platform.runLater(() -> drawBuffer(cLU, visCanvasChannel2, Color.RED, false, -1));
        Platform.runLater(() -> drawBuffer(cRU, visCanvasChannel3, Color.GREEN, false, -1));
        Platform.runLater(() -> drawBuffer(cRL, visCanvasChannel4, Color.ORANGE, false, -1));

        // draw long buffer
        Platform.runLater(() -> drawBuffer(bufferLL.getBuffer(), visBufferLL, Color.BLUE, true, bufferLL.getPosition()));
        Platform.runLater(() -> drawBuffer(bufferLU.getBuffer(), visBufferLU, Color.RED, true, bufferLU.getPosition()));
        Platform.runLater(() -> drawBuffer(bufferRU.getBuffer(), visBufferRU, Color.GREEN, true, bufferRU.getPosition()));
        Platform.runLater(() -> drawBuffer(bufferRL.getBuffer(), visBufferRL, Color.ORANGE, true, bufferRL.getPosition()));

        //threshold
        if(thresholdPassed)
        {
            if(thresholdTimer > thresholdWait)
            {
                circleThresholdPassed.setFill(Color.WHITE);
                thresholdPassed = false;

                // do analyzing
                doAnalyze();

                if(cbSendToAnalyzer.isSelected())
                {
                    // grab latest buffer - samples

                    float[] latestLL = bufferLL.getLatest(thresholdSampleSize);
                    float[] latestLU = bufferLU.getLatest(thresholdSampleSize);
                    float[] latestRU = bufferRU.getLatest(thresholdSampleSize);
                    float[] latestRL = bufferRL.getLatest(thresholdSampleSize);

                    // send to analyzer
                    analyzerController.loadBuffer(
                            new LoopRingBuffer(latestLL),
                            new LoopRingBuffer(latestLU),
                            new LoopRingBuffer(latestRU),
                            new LoopRingBuffer(latestRL));

                    // call threshold analyzer
                    Platform.runLater(() -> analyzerController.runAutoAlgorithm());
                }
            }
            thresholdTimer++;
        }

        Platform.runLater(this::drawLevels);
    }

    private void doAnalyze()
    {
        f = bufferLL.getLatest(thresholdSampleSize);
        g = bufferLU.getLatest(thresholdSampleSize);

        Platform.runLater(() -> drawBuffer(f, visAnalyzing, Color.CYAN, true, -1));
        Platform.runLater(() -> drawBuffer(g, visAnalyzingRight, Color.MAGENTA, true, -1));

        /*
        DelayDetector a = new DelayDetector();
        float corr = a.execCorrelation(f, g);

        float bourke = a.crossCorrelationBourke(f, g, f.length, 100);

        int thr = a.thresholdAnalyzer(f, g, gr.getThreshold(), gr.getGain());

        System.out.print("Threshold: " + thr + " | ");
        System.out.print("Cross Bourke: " + bourke + " | ");
        System.out.print("Cross Exec: " + corr);

        drawTableResult(thr, bourke);

        if(corr > 0)
        {
            System.out.println("\tLEFT");
        }
        else if(corr < 0)
        {
            System.out.println("\tRIGHT");
        }
        else
        {
            System.out.println("\tCenter");
        }
        */
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

        DelayDetector a = new DelayDetector();
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
        bufferLL.saveBuffer("data/plot1.data");
        bufferLU.saveBuffer("data/plot2.data");

        System.out.println("buffer saved!");
    }

    public void btnSave_clicked(ActionEvent actionEvent) {
        //saveAllBuffer();
        LoopRingBuffer flrb = new LoopRingBuffer(f.length+1);
        flrb.put(f);
        flrb.saveBuffer("data/f_buffer.txt");

        LoopRingBuffer glrb = new LoopRingBuffer(g.length+1);
        flrb.put(g);
        flrb.saveBuffer("data/g_buffer.txt");

        System.out.println("buffer saved!");
    }

    public void btnExit_Clicked(ActionEvent actionEvent) {
        if (recorder != null)
            recorder.stop();
        Platform.exit();
    }
}
