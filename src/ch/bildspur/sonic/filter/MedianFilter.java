package ch.bildspur.sonic.filter;

import marf.util.Arrays;

/**
 * Created by cansik on 18/05/16.
 */
public class MedianFilter {

    int filterSize;
    int halfFilter;
    int filterHotspot;

    public MedianFilter(int filterSize) {
        // check if filter size is odd
        assert filterSize / 2 != 0;

        this.filterSize = filterSize;
        this.halfFilter = filterSize / 2;
        this.filterHotspot = filterSize / 2 + 1;
    }

    public float[] filter(float[] data) {
        // wrap with zeros
        float[] buffer = new float[data.length + 2 * halfFilter];
        float[] medianBuffer = new float[filterSize];

        // copy array to buffer
        Arrays.copy(buffer, halfFilter, data, data.length);

        for (int i = halfFilter; i < data.length + halfFilter; i++) {
            int k = i - halfFilter;

            // select values
            for (int j = -halfFilter; j < halfFilter; j++)
                medianBuffer[j + halfFilter] = buffer[i + j];

            // sort
            Arrays.sort(medianBuffer);

            // take middle element
            data[k] = medianBuffer[filterHotspot];
        }

        return data;
    }

    public float[] filterAndStretch(float[] data, float stretchFactor) {
        float[] filteredData = filter(data);
        for (int i = 0; i < filteredData.length; i++)
            filteredData[i] *= (filteredData[i] * stretchFactor);

        return filteredData;
    }
}
