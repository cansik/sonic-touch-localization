package main;

import ch.bildspur.sonic.AudioUtils;
import ch.bildspur.sonic.GestureRecognizer;
import ch.bildspur.sonic.LaneRecorder;
import ch.fhnw.ether.audio.JavaSoundSource;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.io.File;
import java.util.Arrays;

public class Controller implements IBufferReceiver {

    @FXML
    public Canvas visCanvasChannel1;

    @FXML
    public Canvas visCanvasChannel2;

    public void btnTest_clicked(ActionEvent actionEvent) throws InterruptedException {
        System.out.println("draw visualisation");
        //drawTableVisualisation();

        String[] sources = AudioUtils.getSources();

        int channels = 2;
        LaneRecorder recorder = new LaneRecorder(new JavaSoundSource(channels, 48000, 128 * channels));
        recorder.getProgram().addLast(new GestureRecognizer(this));
        recorder.start();

        Thread.sleep(1000);

        recorder.stop();
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

        for(int i = 0; i < buffer.length - 1; i++)
        {
            float v = buffer[i];

            gc.fillOval(x + space * i, y + v * 100, 5, 5);
        }

    }

    @Override
    public void bufferReceived(float[][] channels) {
        float[] c1 = Arrays.copyOf(channels[0], channels[0].length);
        float[] c2 = Arrays.copyOf(channels[1], channels[1].length);

        Platform.runLater(() -> drawBuffer(20, 50, c1, visCanvasChannel1));
        Platform.runLater(() -> drawBuffer(20, 50, c2, visCanvasChannel2));
    }
}
