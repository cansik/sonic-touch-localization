package main.analyzer;

import marf.FeatureExtraction.FeatureExtractionException;
import marf.FeatureExtraction.RawFeatureExtraction.RawFeatureExtraction;
import marf.MARF;
import marf.Preprocessing.Dummy.Raw;
import marf.Preprocessing.Preprocessing;
import marf.Preprocessing.PreprocessingException;
import marf.Storage.Sample;

import static ch.bildspur.sonic.ltm.OneChannelLTM.toDA;

/**
 * Created by cansik on 17/05/16.
 */
public class AudioFeatureExtractor {

    public void extract(float[] f) throws PreprocessingException, FeatureExtractionException {
        Sample sample = new Sample(toDA(f));
        Preprocessing preprocessing = new Raw(sample);
        RawFeatureExtraction rfe = new RawFeatureExtraction(preprocessing);
        rfe.extractFeatures();
        double[] features = rfe.getFeaturesArray();
        System.out.println("features");
    }


}
