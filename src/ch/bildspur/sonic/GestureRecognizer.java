package ch.bildspur.sonic;

import ch.fhnw.ether.audio.AudioFrame;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import main.IBufferReceiver;

/**
 * Created by cansik on 10/04/16.
 */
public class GestureRecognizer extends AbstractRenderCommand<IAudioRenderTarget> {


    IBufferReceiver receiver;

    LoopRingBuffer buffer = new LoopRingBuffer(50000);
    LoopRingBuffer buffer2 = new LoopRingBuffer(50000);

    public GestureRecognizer()
    {
        super();
    }

    public GestureRecognizer(IBufferReceiver receiver)
    {
        this();
        this.receiver = receiver;
    }

    int k = 0;

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

        k += bufferSize;

        if(k > 50000)
        {
            k = 0;
            //buffer.saveBuffer("plot1.data");
            //buffer2.saveBuffer("plot2.data");

            System.out.println("buffer saved!");
        }

        // just some buffer test
        buffer.put(channels[0]);
        buffer2.put(channels[1]);

        receiver.bufferReceived(channels);
    }
}
