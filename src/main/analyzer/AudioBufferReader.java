package main.analyzer;

import ch.bildspur.sonic.LoopRingBuffer;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.render.variable.base.FloatArray;
import ch.fhnw.util.FloatList;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cansik on 10/05/16.
 */
public class AudioBufferReader extends AbstractRenderCommand<IAudioRenderTarget> {
    FloatList buffer;

    public AudioBufferReader()
    {
        buffer = new FloatList();
    }

    @Override
    protected void run(IAudioRenderTarget target) throws RenderCommandException {
        int nChannels = target.getNumChannels();

        float[] samples = target.getFrame().samples;

        for(int i = 0; i < samples.length; i+= nChannels)
            for(int ch = 0; ch < nChannels; ch++)
                buffer.add(samples[i+ch]);
    }

    public float[] getBuffer() {
        return buffer.toArray();
    }
}