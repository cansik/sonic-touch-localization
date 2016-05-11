package ch.bildspur.sonic;

import ch.fhnw.ether.audio.AudioFrame;
import ch.fhnw.ether.audio.AudioUtilities;
import ch.fhnw.ether.audio.FFT;
import org.jtransforms.fft.FloatFFT_1D;

import java.util.Arrays;

/**
 * Created by cansik on 26/04/16.
 */
public class TDOAAnalyzer {


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


    public float execCorrelation(float[] x1, float[] x2)
    {
        // define the size of the resulting correlation field
        int corrSize = 2*x1.length;
        // create correlation vector
        float[] out = new float[corrSize];
        // shift variable
        int shift = x1.length;
        float val;
        int maxIndex = 0;
        float maxVal = 0;

        // we have push the signal from the left to the right
        for(int i=0;i<corrSize;i++)
        {
            val = 0;
            // multiply sample by sample and sum up
            for(int k=0;k<x1.length;k++)
            {
                // x2 has reached his end - abort
                if((k+shift) > (x2.length -1))
                {
                    break;
                }

                // x2 has not started yet - continue
                if((k+shift) < 0)
                {
                    continue;
                }

                // multiply sample with sample and sum up
                val += x1[k] * x2[k+shift];
                //System.out.print("x1["+k+"] * x2["+(k+tmp_tau)+"] + ");
            }
            //System.out.println();
            // save the sample
            out[i] = val;
            shift--;
            // save highest correlation index
            if(out[i] > maxVal)
            {
                maxVal = out[i];
                maxIndex = i;
            }
        }

        // set the delay
        return maxIndex - x1.length;
    }

    /***
     * Detects which channel has first overthrown the threshold.
     * @return 1 if f, -1 if g and 0 if nothing
     */
    public int thresholdAnalyzer(float[] f, float[] g, float threshold, float gain)
    {
        for(int i = 0; i < f.length; i++)
        {
            boolean isF = Math.abs(f[i] / gain) >= threshold;
            boolean isG = Math.abs(g[i] / gain) >= threshold;

            if(isF && isG)
                return 0;

            if(isF)
                return 1;

            if(isG)
                return -1;
        }

        return 0;
    }

    public int extendedThresholdAnalyzer(float[] f, float[] g, float threshold)
    {
        int passPointF = -1;
        int passPointG = -1;

        for(int i = 0; i < f.length; i++)
        {
            boolean isF = Math.abs(f[i]) >= threshold;
            boolean isG = Math.abs(g[i]) >= threshold;

            if(passPointF < 0 && isF)
                passPointF = i;

            if(passPointG < 0 && isG)
                passPointG = i;

            // break if both points found
            if(passPointF >= 0 && passPointG >= 0)
                break;
        }

        return passPointF - passPointG;
    }
}
