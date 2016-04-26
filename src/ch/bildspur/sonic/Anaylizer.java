package ch.bildspur.sonic;

import ch.fhnw.ether.audio.AudioFrame;
import ch.fhnw.ether.audio.AudioUtilities;
import ch.fhnw.ether.audio.FFT;
import org.jtransforms.fft.FloatFFT_1D;

import java.util.Arrays;

/**
 * Created by cansik on 26/04/16.
 */
public class Anaylizer {


    public float[] crossCorrelation(float[] f, float[] g)
    {
        assert(f.length == g.length);

        int padding = f.length;

        //f = addPadding(f, padding);
        //g = addPadding(g, padding);

        float[] ft = doFFT(f);
        float[] gt = doFFT(g);

        float[] fgt = mulitply(ft, gt);

        float[] invfg = doIFFT(fgt);

        return invfg;
    }

    float[] mulitply(float[] f, float[] g)
    {
        float[] r = new float[f.length];
        for(int i = 0; i < f.length; i++)
            r[i] = f[i] * g[i];
        return r;
    }

    float[] doFFT(float[] a)
    {
        FloatFFT_1D fftFl =  new FloatFFT_1D(a.length);

        float[] fft = new float[a.length * 2];
        System.arraycopy(a, 0, fft, 0, a.length);
        fftFl.realForwardFull(fft);

        return fft;
    }

    float[] doIFFT(float[] a)
    {
        FloatFFT_1D fftFl =  new FloatFFT_1D(a.length);

        float[] ifft = new float[a.length * 2];
        System.arraycopy(a, 0, ifft, 0, a.length);
        fftFl.realInverseFull(ifft, true);

        return ifft;
    }

    float[] addPadding(float[] a, int padding)
    {
        return Arrays.copyOf(a, a.length + padding);
    }

    void printArray(float[] a)
    {
        printArray(a, 0, a.length);
    }

    void printArray(float[] a, int start, int end)
    {
        for(int i = start; i < end; i++)
            System.out.println(i + ": " + a[i]);
    }


    public float crossCorrelationBourke(float[] x, float[] y, int n, int maxdelay)
    {
        int i,j;
        double mx,my,sx,sy,sxy,denom,r;
        r = 0;

        /* Calculate the mean of the two series x[], y[] */
        mx = 0;
        my = 0;
        for (i=0;i<n;i++) {
            mx += x[i];
            my += y[i];
        }
        mx /= n;
        my /= n;

   /* Calculate the denominator */
        sx = 0;
        sy = 0;
        for (i=0;i<n;i++) {
            sx += (x[i] - mx) * (x[i] - mx);
            sy += (y[i] - my) * (y[i] - my);
        }
        denom = Math.sqrt(sx*sy);

   /* Calculate the correlation series */
        for (int delay=-maxdelay;delay<maxdelay;delay++) {
            sxy = 0;
            for (i=0;i<n;i++) {
                j = i + delay;
                while (j < 0)
                    j += n;
                j %= n;
                sxy += (x[i] - mx) * (y[j] - my);
            }
            r = sxy / denom;

      /* r is the correlation coefficient at "delay" */

        }

        return (float)r;
    }
}
