package main;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;

/**
 * Created by cansik on 10/05/16.
 */
class Levels extends AbstractRenderCommand<IAudioRenderTarget> {
    float[] levels;

    @Override
    protected void run(IAudioRenderTarget target) throws RenderCommandException {
        int nChannels = target.getNumChannels();
        if(levels == null) levels = new float[nChannels];

        float[] samples = target.getFrame().samples;

        for(int i = 0; i < samples.length; i+= nChannels)
            for(int ch = 0; ch < nChannels; ch++)
                levels[ch] += samples[i+ch];

        for(int ch = 0; ch < nChannels; ch++)
            levels[ch] /= (samples.length / nChannels);
    }

    public float getLevel(int channel) {
        if(levels == null) return 0f;
        return levels[channel];
    }
}
