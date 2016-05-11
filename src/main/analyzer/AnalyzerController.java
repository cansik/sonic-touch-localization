package main.analyzer;

import ch.bildspur.sonic.Anaylizer;
import ch.bildspur.sonic.LoopRingBuffer;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.JavaSoundTarget;
import ch.fhnw.ether.audio.NullAudioTarget;
import ch.fhnw.ether.audio.URLAudioSource;
import ch.fhnw.ether.media.IRenderTarget;
import ch.fhnw.ether.media.IScheduler;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.media.RenderProgram;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import java.io.File;
import java.io.IOException;

/**
 * Created by cansik on 10/05/16.
 */
public class AnalyzerController {
    public Canvas visLeftLower;
    public Canvas visLeftUpper;
    public Canvas visRightUpper;
    public Canvas visRightLower;
    public Canvas visTable;
    public ProgressBar progressBar;
    public Label dataSetName;

    LoopRingBuffer bufferLL;
    LoopRingBuffer bufferLU;
    LoopRingBuffer bufferRU;
    LoopRingBuffer bufferRL;


    public void btnThreshold_Clicked(ActionEvent actionEvent) {
        float[] f = bufferLL.getBuffer();
        float[] g = bufferLU.getBuffer();
        float[] h = bufferRU.getBuffer();
        float[] k = bufferRL.getBuffer();

        // prepare params
        float sonicSpeed = 343.2f; // m/s
        float samplingRate = 44100; // hz
        float tableLength = 2; // m
        float threshold = 0.2f;

        // calculate path percentage
        double leftPer = getPercentagePosition(sonicSpeed, samplingRate, tableLength - 1, threshold, f, g);
        double rightPer = getPercentagePosition(sonicSpeed, samplingRate, tableLength - 1, threshold, k, h);
        double topPer = getPercentagePosition(sonicSpeed, samplingRate, tableLength, threshold, g, h);
        double bottomPer = getPercentagePosition(sonicSpeed, samplingRate, tableLength, threshold, f, k);

        double diagnoal1 = getPercentagePosition(sonicSpeed, samplingRate, (float)Math.sqrt(5), threshold, f, h);
        double diagnoal2 = getPercentagePosition(sonicSpeed, samplingRate, (float)Math.sqrt(5), threshold, g, k);

        // draw result
        GraphicsContext gc = visTable.getGraphicsContext2D();
        gc.clearRect(0, 0, visTable.getWidth(), visTable.getHeight());

        // draw lines
        double width = visTable.getWidth();
        double height = visTable.getHeight();

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
        gc.setStroke(Color.MAGENTA);
        gc.strokeOval(width * bottomPer - hs, height * rightPer - hs, size, size);

        // diagonal 1
        gc.setStroke(Color.ORANGE);
        gc.strokeOval(width * diagnoal1 - hs, height * diagnoal1 - hs, size, size);

        // diagonal 2
        gc.setStroke(Color.LIMEGREEN);
        gc.strokeOval(width * diagnoal2 - hs, height * diagnoal2 - hs, size, size);

        // calculate center point
        double meanX = (width * topPer + width * bottomPer) / 2; //+ width * diagnoal1 + width * diagnoal2) / 4;
        double meanY = (height * rightPer + height * leftPer) / 2; // + height * diagnoal1 + height * diagnoal2) / 4;

        // draw arrow
        gc.setStroke(Color.BLUE);
        gc.strokeLine(meanX, meanY, width / 2, height / 2);

        // draw center
        gc.setStroke(Color.GOLD);
        gc.strokeOval(meanX - hs, meanY - hs, size, size);

        // draw border
        gc.setStroke(Color.BLACK);
        gc.strokeRect(1, 1, width - 2, height - 2);
    }

    public void btnLoad_Clicked(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(((Node)actionEvent.getTarget()).getScene().getWindow());

        if(selectedDirectory == null){
            System.out.println("No directory selected!");
        }else{
            loadData(selectedDirectory);
        }
    }

    double getPercentagePosition(float sonicSpeed, float samplingRate, float tableLength, float threshold, float[] f, float[] g)
    {
        Anaylizer a = new Anaylizer();
        double delta = a.extendedThresholdAnalyzer(f, g, threshold);
        double fullTime = 1/sonicSpeed*tableLength;
        double samplesForDistance = fullTime * samplingRate;
        double sampleWay = (samplesForDistance / 2) + delta;
        return (sampleWay / samplesForDistance);
    }

    void updateProgress(double value)
    {
        Platform.runLater(() -> progressBar.setProgress(progressBar.getProgress() + value));
    }

    void resetProgress()
    {
        Platform.runLater(() -> progressBar.setProgress(0));
    }

    void loadData(File dir)
    {
        resetProgress();
        System.out.println("Loading dataset '" + dir.getName() + "'...");
        dataSetName.setText(dir.getName());

        for(File file : dir.listFiles())
        {
            if(file.getName().equals("LL.wav"))
                bufferLL = loadWave(file);

            if(file.getName().equals("LU.wav"))
                bufferLU = loadWave(file);

            if(file.getName().equals("RU.wav"))
                bufferRU = loadWave(file);

            if(file.getName().equals("RL.wav"))
                bufferRL = loadWave(file);
        }

        // set mimium length of all
        int min = Integer.MAX_VALUE;

        if(min > bufferLL.size()) min = bufferLL.size();
        if(min > bufferLU.size()) min = bufferLU.size();
        if(min > bufferRU.size()) min = bufferRU.size();
        if(min > bufferRL.size()) min = bufferRL.size();

        // resize arrays
        bufferLL = new LoopRingBuffer(bufferLL, min);
        bufferLU = new LoopRingBuffer(bufferLU, min);
        bufferRU = new LoopRingBuffer(bufferRU, min);
        bufferRL = new LoopRingBuffer(bufferRL, min);

        //calculate size factor for normalisation
        float max = 0;
        for(int i = 0; i < bufferLL.size(); i++)
        {
            if(max < bufferLL.get(i)) max = bufferLL.get(i);
            if(max < bufferLU.get(i)) max = bufferLU.get(i);
            if(max < bufferRU.get(i)) max = bufferRU.get(i);
            if(max < bufferRL.get(i)) max = bufferRL.get(i);
        }
        float gainFactor = 1.0f / max;

        // visualize data
        drawBuffer(bufferLL.getBuffer(), visLeftLower, Color.BLUE, gainFactor);
        drawBuffer(bufferLU.getBuffer(), visLeftUpper, Color.RED, gainFactor);
        drawBuffer(bufferRU.getBuffer(), visRightUpper, Color.GREEN, gainFactor);
        drawBuffer(bufferRL.getBuffer(), visRightLower, Color.ORANGE, gainFactor);
    }

    void drawBuffer(float[] buffer, Canvas c, Color color)
    {
        drawBuffer(buffer, c, color, 1.0f);
    }

    void drawBuffer(float[] buffer, Canvas c, Color color, float gainFactor)
    {
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.clearRect(0, 0, c.getWidth(), c.getHeight());
        float space = (float)(c.getWidth() / buffer.length);

        gc.setFill(color);

        float y = (float)c.getHeight() / 2f;

        for(int i = 0; i < buffer.length - 1; i++)
        {
            float v = buffer[i];

            gc.fillOval(space * i, y + (y * v * gainFactor), 1, 1);
        }

        // draw border
        gc.setStroke(Color.BLACK);
        gc.strokeRect(1, 1, c.getWidth() - 2, c.getHeight() - 2);
    }

    LoopRingBuffer loadWave(File waveFile)
    {
        System.out.print("loading " + waveFile.getName() + "...");
        LoopRingBuffer lrb = null;

        try {
            // create source
            URLAudioSource source = new URLAudioSource(waveFile.toURL());
            long time = (long) Math.ceil(source.getLengthInSeconds() * 1000);
            AudioBufferReader bufferReader = new AudioBufferReader();
            RenderProgram<IAudioRenderTarget> program = new RenderProgram<>(source, bufferReader);

            // run audio
            IAudioRenderTarget target = new JavaSoundTarget();
            target.useProgram(program);

            target.start();
            Thread.sleep(time);
            //target.sleepUntil(time);
            target.stop();

            //read buffer
            lrb = new LoopRingBuffer(bufferReader.getBuffer());

        } catch (IOException e) {
            e.printStackTrace();
        } catch (RenderCommandException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("loaded!");

        return lrb;
    }

    public void visMouse_Moved(MouseEvent event) {
        Canvas c = (Canvas)event.getSource();
        int i = (int)(event.getX() / c.getWidth() * bufferLL.size());
        System.out.println(event.getX() + ": " + bufferLL.get(i));
    }
}
