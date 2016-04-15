package ch.bildspur.sonic;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.JavaSoundSource;
import ch.fhnw.ether.audio.JavaSoundTarget;
import ch.fhnw.ether.audio.NullAudioTarget;
import ch.fhnw.ether.audio.fx.AudioGain;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.IScheduler;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.media.RenderProgram;
import ch.fhnw.ether.ui.ParameterWindow;
import ch.fhnw.util.net.osc.ExceptionHandler;

/**
 * Created by cansik on 10/04/16.
 */
public class LaneRecorder {

    private volatile boolean recording = false;
    private RenderProgram<IAudioRenderTarget> program;
    private JavaSoundSource source;

    public LaneRecorder(JavaSoundSource source, AbstractRenderCommand<IAudioRenderTarget>... commands)
    {
        this.source = source;
        this.program = new RenderProgram<>(source, commands);
    }

    public void stop()
    {
        recording = false;
    }

    public void start()
    {
        recording = true;
        new ParameterWindow(program);

        NullAudioTarget audioOut = new NullAudioTarget(source.getNumChannels(), source.getSampleRate());

        try {
            audioOut.useProgram(program);
            audioOut.start();

            while(recording)
            {
                //audioOut.render();
                audioOut.sleepUntil(IScheduler.NOT_RENDERING);
            }

            audioOut.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isRecording() {
        return recording;
    }

    public RenderProgram<IAudioRenderTarget> getProgram() {
        return program;
    }
}
