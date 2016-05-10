package main.analyzer;

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
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
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

    LoopRingBuffer bufferLL;
    LoopRingBuffer bufferLU;
    LoopRingBuffer bufferRU;
    LoopRingBuffer bufferRL;

    public void btnLoad_Clicked(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(((Node)actionEvent.getTarget()).getScene().getWindow());

        if(selectedDirectory == null){
            System.out.println("No directory selected!");
        }else{
            Platform.runLater(() -> loadData(selectedDirectory));
        }
    }

    void loadData(File dir)
    {
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

        // visualize data
        drawBuffer(bufferLL.getBuffer(), visLeftLower, Color.BLUE);
        drawBuffer(bufferLU.getBuffer(), visLeftUpper, Color.RED);
        drawBuffer(bufferRU.getBuffer(), visRightUpper, Color.GREEN);
        drawBuffer(bufferRL.getBuffer(), visRightLower, Color.ORANGE);
    }

    void drawBuffer(float[] buffer, Canvas c, Color color)
    {
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.clearRect(0, 0, c.getWidth(), c.getHeight());
        float space = (float)(c.getWidth() / buffer.length);

        gc.setFill(color);

        float y = (float)c.getHeight() / 2f;

        for(int i = 0; i < buffer.length - 1; i++)
        {
            float v = buffer[i];

            gc.fillOval(space * i, y + v, 3, 3);
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
}
