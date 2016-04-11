package ch.bildspur.sonic;

import ch.fhnw.ether.audio.AudioFrame;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import main.IBufferReceiver;

import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by cansik on 10/04/16.
 */
public class GestureRecognizer extends AbstractRenderCommand<IAudioRenderTarget> {


    IBufferReceiver receiver;

    public GestureRecognizer()
    {
        super();
    }

    public GestureRecognizer(IBufferReceiver receiver)
    {
        this();
        this.receiver = receiver;
    }


    @Override
    protected void run(IAudioRenderTarget target) throws RenderCommandException {
        AudioFrame frame = target.getFrame();
        float[] samples = frame.getMonoSamples();

        // split samples into channels
        int bufferSize = samples.length / frame.nChannels;
        float[][] channels = new float[frame.nChannels][bufferSize];

        for(int c = 1; c <= frame.nChannels; c++)
        {
            for(int i = 0; i < bufferSize; i++) {
                channels[c - 1][i] = samples[c * i];
            }
        }

        receiver.bufferReceived(channels);
    }
}
