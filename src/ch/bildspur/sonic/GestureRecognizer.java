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

    float threshold = 0.5f;

    float gain = 0;

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

    public float getGain() {
        return gain;
    }

    public void setGain(float gain) {
        this.gain = gain;
    }

    public float getThreshold() {
        return threshold;
    }

    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }

    @Override
    protected void run(IAudioRenderTarget target) throws RenderCommandException {
        AudioFrame frame = target.getFrame();
        float[] samples = frame.samples;

        // split samples into channels
        int bufferSize = samples.length / frame.nChannels;
        float[][] channels = new float[frame.nChannels][bufferSize];

        for(int i = 0; i < samples.length; i+= frame.nChannels)
            for(int ch = 0; ch < frame.nChannels; ch++)
                channels[ch][(i+ch) / frame.nChannels] = samples[i+ch];

        checkThreshold(channels, threshold);

        receiver.bufferReceived(channels);

        k += bufferSize;
    }

    private void checkThreshold(float[][] channels, float threshold)
    {
        for(int c = 0; c < channels.length; c++)
        {
            for(int i = 0; i < channels[c].length; i++)
            {
                if(Math.abs(channels[c][i] / gain) > threshold)
                {
                    receiver.thresholdPassed();
                }
            }
        }
    }
}
