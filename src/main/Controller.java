package main;

import ch.bildspur.sonic.*;
import ch.fhnw.ether.audio.JavaSoundSource;
import ch.fhnw.ether.audio.fx.AudioGain;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import javax.swing.*;
import java.util.Arrays;

public class Controller implements IBufferReceiver {

    @FXML
    public Canvas visCanvasChannel1;

    @FXML
    public Canvas visCanvasChannel2;

    @FXML
    public Canvas visCanvasChannel3;

    @FXML
    public Canvas visCanvasChannel4;

    GestureRecognizer gr;

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

        SwingUtilities.invokeLater(recorder::start);
    }

    void drawTableVisualisation() {
        GraphicsContext gc = visCanvasChannel1.getGraphicsContext2D();

        gc.setFill(Color.BLUE);
        gc.rect(10, 10, 200, 100);

        gc.setFill(Color.GREEN);
        gc.fill();
    }

    void drawBuffer(float x, float y, float[] buffer, Canvas c)
    {
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.clearRect(0, 0, 300, 200);
        float space = 5;

        gc.setFill(Color.BLUE);

        gc.strokeRect(x, y - 20, space * (buffer.length - 1), 50);

        float postAmp = 100000;

        for(int i = 0; i < buffer.length - 1; i++)
        {
            float v = buffer[i];

            gc.fillOval(x + space * i, y + v * postAmp, 5, 5);
        }

    }

    @Override
    public void bufferReceived(float[][] channels) {
        float[] c1 = Arrays.copyOf(channels[0], channels[0].length);
        float[] c2 = Arrays.copyOf(channels[1], channels[1].length);

        Platform.runLater(() -> drawBuffer(20, 50, c1, visCanvasChannel1));
        Platform.runLater(() -> drawBuffer(20, 50, c2, visCanvasChannel2));


        /*
        float[] c3 = Arrays.copyOf(channels[2], channels[2].length);
        float[] c4 = Arrays.copyOf(channels[3], channels[3].length);
        Platform.runLater(() -> drawBuffer(20, 50, c3, visCanvasChannel3));
        Platform.runLater(() -> drawBuffer(20, 50, c4, visCanvasChannel4));
        */

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

        cross = new float[f.length];
        for(int i = 0; i < cross.length; i++)
        {
            cross[i] = a.crossCorrelationBourke(f, g, i, 100);
        }

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

    public void btnSave_clicked(ActionEvent actionEvent) {
        gr.saveAllBuffer();
    }
}
