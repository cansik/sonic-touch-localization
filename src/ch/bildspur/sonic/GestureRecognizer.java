package ch.bildspur.sonic;

import ch.fhnw.ether.audio.AudioFrame;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import main.IGestureHandler;

/**
 * Created by cansik on 10/04/16.
 */
public class GestureRecognizer extends AbstractRenderCommand<IAudioRenderTarget> {

    IGestureHandler receiver;

    int k = 0;

    public GestureRecognizer()
    {
        super();
    }

    public GestureRecognizer(IGestureHandler receiver)
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

        checkThreshold(channels, 0.5f);

        receiver.bufferReceived(channels);

        k += bufferSize;
    }

    private void checkThreshold(float[][] channels, float threshold)
    {

    }
}
