package ch.bildspur.sonic.ltm.util;

/*
 *  Copyright 2006 Columbia University.
 *
 *  This file is part of MEAPsoft.
 *
 *  MEAPsoft is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2 as
 *  published by the Free Software Foundation.
 *
 *  MEAPsoft is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MEAPsoft; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA
 *
 *  See the file "COPYING" for the text of the license.
 */

import java.util.Arrays;

import java.util.Arrays;
import java.io.*;
import javax.sound.sampled.*;

/*
 * Library of basic DSP algorithms.  Most of these have analogs in
 * Matlab with the same name.
 *
 * This code only operates on real valued data.
 *
 * @author Ron Weiss (ronw@ee.columbia.edu)
 */
public class DSP
{
    /**
     * Convolves sequences a and b.  The resulting convolution has
     * length a.length+b.length-1.
     */
    public static double[] conv(double[] a, double[] b)
    {
        double[] y = new double[a.length+b.length-1];

        // make sure that a is the shorter sequence
        if(a.length > b.length)
        {
            double[] tmp = a;
            a = b;
            b = tmp;
        }

        for(int lag = 0; lag < y.length; lag++)
        {
            y[lag] = 0;

            // where do the two signals overlap?
            int start = 0;
            // we can't go past the left end of (time reversed) a
            if(lag > a.length-1)
                start = lag-a.length+1;

            int end = lag;
            // we can't go past the right end of b
            if(end > b.length-1)
                end = b.length-1;

            //System.out.println("lag = " + lag +": "+ start+" to " + end);
            for(int n = start; n <= end; n++)
            {
                //System.out.println("  ai = " + (lag-n) + ", bi = " + n);
                y[lag] += b[n]*a[lag-n];
            }
        }

        return(y);
    }

    /**
     * Computes the cross correlation between sequences a and b.
     */
    public static double[] xcorr(double[] a, double[] b)
    {
        int len = a.length;
        if(b.length > a.length)
            len = b.length;

        return xcorr(a, b, len-1);

        // // reverse b in time
        // double[] brev = new double[b.length];
        // for(int x = 0; x < b.length; x++)
        //     brev[x] = b[b.length-x-1];
        //
        // return conv(a, brev);
    }

    /**
     * Computes the auto correlation of a.
     */
    public static double[] xcorr(double[] a)
    {
        return xcorr(a, a);
    }

    /**
     * Computes the cross correlation between sequences a and b.
     * maxlag is the maximum lag to
     */
    public static double[] xcorr(double[] a, double[] b, int maxlag)
    {
        double[] y = new double[2*maxlag+1];
        Arrays.fill(y, 0);

        for(int lag = b.length-1, idx = maxlag-b.length+1;
            lag > -a.length; lag--, idx++)
        {
            if(idx < 0)
                continue;

            if(idx >= y.length)
                break;

            // where do the two signals overlap?
            int start = 0;
            // we can't start past the left end of b
            if(lag < 0)
            {
                //System.out.println("b");
                start = -lag;
            }

            int end = a.length-1;
            // we can't go past the right end of b
            if(end > b.length-lag-1)
            {
                end = b.length-lag-1;
                //System.out.println("a "+end);
            }

            //System.out.println("lag = " + lag +": "+ start+" to " + end+"   idx = "+idx);
            for(int n = start; n <= end; n++)
            {
                //System.out.println("  bi = " + (lag+n) + ", ai = " + n);
                y[idx] += a[n]*b[lag+n];
            }
            //System.out.println(y[idx]);
        }

        return(y);
    }

    /**
     * Filters x by the IIR filter defined by a and b.  Similar to
     * Matlab's filter().
     *
     * This is a direct implementation of the corresponding difference
     * equation.
     */
    public static double[] filter(double[] b, double[] a, double[] x)
    {
        double[] y = new double[x.length];

        // factor out a[0]
        if(a[0] != 1)
        {
            for(int ia = 1; ia < a.length; ia++)
                a[ia] = a[ia]/a[0];

            for(int ib = 0; ib < b.length; ib++)
                b[ib] = b[ib]/a[0];
        }

        for(int t = 0; t < x.length; t++)
        {
            y[t] = 0;

            // input terms
            int len = b.length-1 < t ? b.length-1 : t;
            for(int ib = 0; ib <= len; ib++)
                y[t] += b[ib]*x[t-ib];

            // output terms
            len = a.length-1 < t ? a.length-1 : t;
            for(int ia = 1; ia <= len; ia++)
                y[t] -= a[ia]*y[t-ia];
        }

        return y;
    }

    /**
     * Returns an n point symmetric Hanning window.
     *
     * Like the Matlab hanning() function (and unlike the version
     * included in Octave), this drops the first and last zero
     * samples.  So hanning(10) in Octave is the same as hanning(8) in
     * Matlab and DSP.hanning(8).
     */
    public static double[] hanning(int n)
    {
        double[] wind = new double[n];

        if(n == 1)
            wind[0] = 1;
        else
            for(int x = 1; x < n+1; x++)
                wind[x-1] = 0.5*(1 - Math.cos(2*Math.PI*x/(n+1)));

        return wind;
    }

    /**
     * Returns the inner product of a and b.  a and b should be the
     * same length or bad things will happen.
     */
    public static double dot(double[] a, double[] b)
    {
        double y = 0;

        for(int x = 0; x < a.length; x++)
            y += a[x]*b[x];

        return y;
    }

    /**
     * Returns the elementwise product of a and b.  a and b should be
     * the same length or bad things will happen.
     */
    public static double[] times(double[] a, double[] b)
    {
        double[] y = new double[a.length];

        for(int x = 0; x < y.length; x++)
            y[x] = a[x]*b[x];

        return y;
    }

    /**
     * Multiplies each element of a by b.
     */
    public static double[] times(double[] a, double b)
    {
        double[] y = new double[a.length];

        for(int x = 0; x < y.length; x++)
            y[x] = a[x]*b;

        return y;
    }

    /**
     * Returns the elementwise quotient of a divided by b (a./b).  a
     * and b should be the same length or bad things will happen.
     */
    public static double[] rdivide(double[] a, double[] b)
    {
        double[] y = new double[a.length];

        for(int x = 0; x < y.length; x++)
            y[x] = a[x]/b[x];

        return y;
    }

    /**
     * Divides each element of a by b.
     */
    public static double[] rdivide(double[] a, double b)
    {
        double[] y = new double[a.length];

        for(int x = 0; x < y.length; x++)
            y[x] = a[x]/b;

        return y;
    }

    /**
     * Returns the elementwise sum of a and b.  a and b should be
     * the same length or bad things will happen.
     */
    public static double[] plus(double[] a, double[] b)
    {
        double[] y = new double[a.length];

        for(int x = 0; x < y.length; x++)
            y[x] = a[x]+b[x];

        return y;
    }

    /**
     * Adds b to each element of a.
     */
    public static double[] plus(double[] a, double b)
    {
        double[] y = new double[a.length];

        for(int x = 0; x < y.length; x++)
            y[x] = a[x]+b;

        return y;
    }

    /**
     * Returns the elementwise difference of a and b.  a and b should
     * be the same length or bad things will happen.
     */
    public static double[] minus(double[] a, double[] b)
    {
        double[] y = new double[a.length];

        for(int x = 0; x < y.length; x++)
            y[x] = a[x]-b[x];

        return y;
    }

    /**
     * Subtracts b from each element of a (y[x] = a[x]-b).
     */
    public static double[] minus(double[] a, double b)
    {
        double[] y = new double[a.length];

        for(int x = 0; x < y.length; x++)
            y[x] = a[x]-b;

        return y;
    }

    /**
     * Subtracts b from each element of A (Y[x][y] = A[x][y]-b).
     */
    public static double[][] minus(double[][] A, double b)
    {
        double[][] Y = new double[A.length][A[0].length];

        for(int x = 0; x < Y.length; x++)
            for(int y = 0; x < Y[x].length; y++)
                Y[x][y] = A[x][y]-b;

        return Y;
    }

    /**
     * Subtracts each element of b from a (y[x] = a - b[x]).
     */
    public static double[] minus(double a, double[] b)
    {
        double[] y = new double[b.length];

        for(int x = 0; x < y.length; x++)
            y[x] = a-b[x];

        return y;
    }

    /**
     * Returns the sum of the contents of a.
     */
    public static double sum(double[] a)
    {
        double y = 0;

        for(int x = 0; x < a.length; x++)
            y += a[x];

        return y;
    }

    /**
     * Returns the max element of a.
     */
    public static double max(double[] a)
    {
        double y = Double.MIN_VALUE;

        for(int x = 0; x < a.length; x++)
            if(a[x] > y)
                y = a[x];

        return y;
    }

    /**
     * Returns a new array where each element is the maximum of a[i]
     * and b[i].  a and b should be the same length.
     */
    public static double[] max(double[] a, double[] b)
    {
        double[] y = new double[a.length];

        for(int x = 0; x < a.length; x++)
        {
            if(a[x] > b[x])
                y[x] = a[x];
            else
                y[x] = b[x];
        }

        return y;
    }

    /**
     * Returns a new array where each element is the maximum of a[i]
     * and b.
     */
    public static double[] max(double[] a, double b)
    {
        double[] y = new double[a.length];

        for(int x = 0; x < a.length; x++)
        {
            if(a[x] > b)
                y[x] = a[x];
            else
                y[x] = b;
        }

        return y;
    }

    /**
     * Returns a new array where each element is the maximum of
     * a[i][j] and b.
     */
    public static double[][] max(double[][] a, double b)
    {
        double[][] y = new double[a.length][a[0].length];

        for(int r = 0; r < a.length; r++)
        {
            for(int c = 0; c < a[r].length; c++)
            {
                if(a[r][c] > b)
                    y[r][c] = a[r][c];
                else
                    y[r][c] = b;
            }
        }

        return y;
    }

    /**
     * Returns an array of the max elements of each column of a.
     */
    public static double[] max(double[][] a)
    {
        double[] y = new double[a.length];
        Arrays.fill(y, Double.MIN_VALUE);

        for(int r = 0; r < a.length; r++)
            for(int c = 0; c < a[r].length; c++)
                if(a[r][c] > y[r])
                    y[r] = a[r][c];

        return y;
    }

    /**
     * Returns the min element of a.
     */
    public static double min(double[] a)
    {
        double y = Double.MAX_VALUE;

        for(int x = 0; x < a.length; x++)
            if(a[x] < y)
                y = a[x];

        return y;
    }

    /**
     * Returns a new array where each element is the minimum of a[i]
     * and b[i].  a and b should be the same length.
     */
    public static double[] min(double[] a, double[] b)
    {
        double[] y = new double[a.length];

        for(int x = 0; x < a.length; x++)
        {
            if(a[x] < b[x])
                y[x] = a[x];
            else
                y[x] = b[x];
        }

        return y;
    }


    /**
     * Returns a new array where each element is the minimum of a[i]
     * and b.
     */
    public static double[] min(double[] a, double b)
    {
        double[] y = new double[a.length];

        for(int x = 0; x < a.length; x++)
        {
            if(a[x] < b)
                y[x] = a[x];
            else
                y[x] = b;
        }

        return y;
    }

    /**
     * Returns a new array where each element is the minimum of
     * a[i][j] and b.
     */
    public static double[][] min(double[][] a, double b)
    {
        double[][] y = new double[a.length][a[0].length];

        for(int r = 0; r < a.length; r++)
        {
            for(int c = 0; c < a[r].length; c++)
            {
                if(a[r][c] < b)
                    y[r][c] = a[r][c];
                else
                    y[r][c] = b;
            }
        }

        return y;
    }


    /**
     * Returns an array of the min elements of each column of a.
     */
    public static double[] min(double[][] a)
    {
        double[] y = new double[a.length];
        Arrays.fill(y, Double.MAX_VALUE);

        for(int r = 0; r < a.length; r++)
            for(int c = 0; c < a[r].length; c++)
                if(a[r][c] < y[r])
                    y[r] = a[r][c];

        return y;
    }


    /**
     * Returns the index of the max element of a.
     */
    public static int argmax(double[] a)
    {
        double y = Double.MIN_VALUE;
        int idx = -1;

        for(int x = 0; x < a.length; x++)
        {
            if(a[x] > y)
            {
                y = a[x];
                idx = x;
            }
        }

        return idx;
    }

    /**
     * Returns the index of the min element of a.
     */
    public static int argmin(double[] a)
    {
        double y = Double.MAX_VALUE;
        int idx = -1;

        for(int x = 0; x < a.length; x++)
        {
            if(a[x] < y)
            {
                y = a[x];
                idx = x;
            }
        }

        return idx;
    }

    /**
     * Returns the slice of array a between start and end inclusive.
     */
    public static double[] slice(double[] a, int start, int end)
    {
        start = Math.max(start, 0);
        end = Math.min(end, a.length-1);

        double[] y = new double[end-start+1];

        for(int x = start, iy = 0; x <= end; x++, iy++)
            y[iy] = a[x];

        return y;
    }

    /**
     * Returns an array as follows:
     *  {start, start+1, ... , end-1, end}
     */
    public static double[] range(int start, int end)
    {
        return range(start, end, 1);

        // double[] y = new double[end-start+1];
        //
        // for(int x = 0, num = start; x < y.length; x++, num++)
        //     y[x] = num;
        //
        // return y;
    }

    /**
     * Returns an array as follows:
     *   {start, start+increment, ... , end-increment, end}
     */
    public static double[] range(int start, int end, int increment)
    {
        double[] y = new double[1+(end-start)/increment];

        for(int x = 0, num = start; x < y.length; x++, num += increment)
            y[x] = num;

        return y;
    }

    /**
     * Returns an array of ints as follows:
     *   {start, start+increment, ... , end-increment, end}
     */
    public static int[] irange(int start, int end, int increment)
    {
        int[] y = new int[1+(end-start)/increment];

        for(int x = 0, num = start; x < y.length; x++, num += increment)
            y[x] = num;

        return y;
    }

    /**
     * Returns an array of ints as follows:
     *  {start, start+1, ... , end-1, end}
     */
    public static int[] irange(int start, int end)
    {
        return irange(start, end, 1);
    }

    /**
     * Index into array a using the indices listed in idx.
     */
    public static double[] subsref(double[] a, int[] idx)
    {
        double[] y = new double[idx.length];

        for(int x = 0; x < idx.length; x++)
            y[x] = a[idx[x]];

        return y;
    }

    /**
     * Index into array a using the binary array idx.
     */
    public static double[] subsref(double[] a, byte[] idx)
    {
        return subsref(a, find(idx));
    }

    /**
     * Returns an array containing the indices into a that contain 1s.
     */
    public static int[] find(byte[] a)
    {
        int[] v = new int[20];

        int idx = 0;
        for(int x = 0; x < a.length; x++)
        {
            if(a[x] == 1)
            {
                //System.out.println(x+" "+a[x]+" "+v.length);

                v[idx++] = x;

                if(idx == v.length)
                {
                    int[] tmp = new int[2*v.length];

                    for(int i = 0; i < v.length; i++)
                        tmp[i] = v[i];

                    v = tmp;
                }
            }
        }

        // but v might be too big:
        int[] tmp = new int[idx];
        for(int i = 0; i < tmp.length; i++)
            tmp[i] = v[i];

        //System.out.println(tmp.length);

        return tmp;
    }

    /**
     * Returns a binary array with 1s where a[idx] < b[idx] and zeros
     * elsewhere.  a and b should be the same length.
     */
    public static byte[] lt(double[] a, double[] b)
    {
        byte[] y = new byte[a.length];

        for(int x = 0; x < y.length; x++)
        {
            if(a[x] < b[x])
                y[x] = 1;
            else
                y[x] = 0;
        }

        return y;
    }

    /**
     * Returns a binary array with 1s where a[idx] < b and zeros
     * elsewhere.  a and b should be the same length.
     */
    public static byte[] lt(double[] a, double b)
    {
        byte[] y = new byte[a.length];

        for(int x = 0; x < y.length; x++)
        {
            if(a[x] < b)
                y[x] = 1;
            else
                y[x] = 0;
        }

        return y;
    }

    /**
     * Returns a binary array with 1s where a[idx] <= b[idx] and zeros
     * elsewhere.  a and b should be the same length.
     */
    public static byte[] le(double[] a, double[] b)
    {
        byte[] y = new byte[a.length];

        for(int x = 0; x < y.length; x++)
        {
            if(a[x] <= b[x])
                y[x] = 1;
            else
                y[x] = 0;
        }

        return y;
    }

    /**
     * Returns a binary array with 1s where a[idx] <= b and zeros
     * elsewhere.  a and b should be the same length.
     */
    public static byte[] le(double[] a, double b)
    {
        byte[] y = new byte[a.length];

        for(int x = 0; x < y.length; x++)
        {
            if(a[x] <= b)
                y[x] = 1;
            else
                y[x] = 0;
        }

        return y;
    }

    /**
     * Returns a binary array with 1s where a[idx] > b[idx] and zeros
     * elsewhere.  a and b should be the same length.
     */
    public static byte[] gt(double[] a, double[] b)
    {
        byte[] y = new byte[a.length];

        for(int x = 0; x < y.length; x++)
        {
            if(a[x] > b[x])
                y[x] = 1;
            else
                y[x] = 0;
        }

        return y;
    }

    /**
     * Returns a binary array with 1s where a[idx] > b and zeros
     * elsewhere.  a and b should be the same length.
     */
    public static byte[] gt(double[] a, double b)
    {
        byte[] y = new byte[a.length];

        for(int x = 0; x < y.length; x++)
        {
            if(a[x] > b)
                y[x] = 1;
            else
                y[x] = 0;
        }

        return y;
    }

    /**
     * Returns a binary array with 1s where a[idx] >= b[idx] and zeros
     * elsewhere.  a and b should be the same length.
     */
    public static byte[] ge(double[] a, double[] b)
    {
        byte[] y = new byte[a.length];

        for(int x = 0; x < y.length; x++)
        {
            if(a[x] >= b[x])
                y[x] = 1;
            else
                y[x] = 0;
        }

        return y;
    }

    /**
     * Returns a binary array with 1s where a[idx] >= b and zeros
     * elsewhere.  a and b should be the same length.
     */
    public static byte[] ge(double[] a, double b)
    {
        byte[] y = new byte[a.length];

        for(int x = 0; x < y.length; x++)
        {
            if(a[x] >= b)
                y[x] = 1;
            else
                y[x] = 0;
        }

        return y;
    }

    /**
     * Returns the lower median value contained in a.
     */
    public static double median(double[] a)
    {
        // I don't want to sort the original array...  DSP.slice will
        // copy it.
        double[] tmp = slice(a, 0, a.length-1);

        Arrays.sort(tmp);

        return tmp[(int)tmp.length/2];
    }

    /**
     * Returns the mean of a.
     */
    public static double mean(double[] a)
    {
        return sum(a)/a.length;
    }

    /**
     * Returns the mean of each column of A.
     */
    public static double[] mean(double[][] A)
    {
        double[] m = new double[A.length];
        int ncols = A[0].length;

        for(int x = 0; x < A.length; x++)
            for(int y = 0; y < A[x].length; y++)
                m[x] += A[x][y]/ncols;

        return m;
    }


    /**
     * Returns the transpose of the matrix a.
     */
    public static double[][] transpose(double[][] a)
    {
        double[][] y = new double[a[0].length][a.length];

        for(int i = 0; i < a.length; i++)
            for(int j = 0; j < a[i].length; j++)
                y[j][i] = a[i][j];

        return y;
    }

    /**
     * Round each element of a.
     */
    public static double[] round(double[] a)
    {
        double[] y = new double[a.length];

        for(int x = 0; x < y.length; x++)
            y[x] = Math.round(a[x]);

        return y;
    }

    /**
     * Returns the absolute value of each element of a.
     */
    public static double[] abs(double[] a)
    {
        double[] y = new double[a.length];

        for(int x = 0; x < y.length; x++)
            y[x] = Math.abs(a[x]);

        return y;
    }

    /**
     * Cosine of each element of a.
     */
    public static double[] cos(double[] a)
    {
        double[] y = new double[a.length];

        for(int x = 0; x < y.length; x++)
            y[x] = Math.cos(a[x]);

        return y;
    }

    /**
     * Sine of each element of a.
     */
    public static double[] sin(double[] a)
    {
        double[] y = new double[a.length];

        for(int x = 0; x < y.length; x++)
            y[x] = Math.sin(a[x]);

        return y;
    }

    /**
     * Raise each element of a to the b'th power.  Like MATLAB's .^
     * operator.
     */
    public static double[] power(double[] a, double b)
    {
        double[] y = new double[a.length];

        for(int x = 0; x < y.length; x++)
            y[x] = Math.pow(a[x], b);

        return y;
    }

    /**
     * Take the natural log of each element of a.
     */
    public static double[] log(double[] a)
    {
        double[] y = new double[a.length];

        for(int x = 0; x < y.length; x++)
            y[x] = Math.log(a[x]);

        return y;
    }

    /**
     * Take the log base 10 of each element of a.
     */
    public static double[] log10(double[] a)
    {
        double[] y = new double[a.length];
        double log10 = Math.log(10);

        for(int x = 0; x < y.length; x++)
            y[x] = Math.log(a[x])/log10;

        return y;
    }

    /**
     * Take the natural exp of each element of a.
     */
    public static double[] exp(double[] a)
    {
        double[] y = new double[a.length];

        for(int x = 0; x < y.length; x++)
            y[x] = Math.exp(a[x]);

        return y;
    }

    public static double[] todouble(byte[] a)
    {
        double[] y = new double[a.length];

        for(int x = 0; x < y.length; x++)
            y[x] = (double)a[x];

        return y;
    }

    public static double[] todouble(int[] a)
    {
        double[] y = new double[a.length];

        for(int x = 0; x < y.length; x++)
            y[x] = (double)a[x];

        return y;
    }

    /**
     * Set n'th column of matrix A to b
     */
    public static void setColumn(double[][] A, int n, double[] b)
    {
        for(int i=0; i < b.length; i++)
            A[i][n] = b[i];
    }

    /**
     * Returns the n'th column of matrix A
     */
    public static double[] getColumn(double[][] A, int n)
    {
        double[] y = new double[A.length];

        for(int i=0; i < A.length; i++)
            y[i] = A[i][n];

        return y;
    }
    public static void printArray(double[] a)
    {
        for(int x = 0; x < a.length; x++)
            System.out.print(a[x]+" ");
        System.out.println("");
    }

    public static void printArray(double[] a, String name)
    {
        System.out.print("name = ");

        printArray(a);
    }
}