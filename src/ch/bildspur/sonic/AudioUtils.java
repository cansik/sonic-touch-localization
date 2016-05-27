package ch.bildspur.sonic;

import ch.fhnw.util.Pair;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by cansik on 10/04/16.
 */
public class AudioUtils {
    public static final List<Pair<Mixer.Info, Line.Info>> sources = new ArrayList();

    public static String[] getSources() {
        int idx;
        if(sources.isEmpty()) {
            Mixer.Info[] result = AudioSystem.getMixerInfo();
            idx = result.length;

            for(int var2 = 0; var2 < idx; ++var2) {
                Mixer.Info src = result[var2];
                Mixer mixer = AudioSystem.getMixer(src);
                Throwable var5 = null;

                try {
                    javax.sound.sampled.Line.Info[] var6 = mixer.getTargetLineInfo();
                    int var7 = var6.length;

                    for(int var8 = 0; var8 < var7; ++var8) {
                        javax.sound.sampled.Line.Info lineInfo = var6[var8];
                        if(TargetDataLine.class.isAssignableFrom(lineInfo.getLineClass())) {
                            sources.add(new Pair(src, lineInfo));
                            System.out.println(src.getName() + "\t=\t");
                        }
                    }
                } catch (Throwable var17) {
                    var5 = var17;
                    throw var17;
                } finally {
                    if(mixer != null) {
                        if(var5 != null) {
                            try {
                                mixer.close();
                            } catch (Throwable var16) {
                                var5.addSuppressed(var16);
                            }
                        } else {
                            mixer.close();
                        }
                    }

                }
            }
        }

        String[] var19 = new String[sources.size()];
        idx = 0;

        Pair var21;
        for(Iterator var20 = sources.iterator(); var20.hasNext(); var19[idx++] = ((Mixer.Info)var21.first).getName()) {
            var21 = (Pair)var20.next();
        }

        return var19;
    }

    public static float[] normalize(float[] data) {
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;

        // find min & max
        for (float f : data) {
            if (f < min)
                min = f;
            if (f > max)
                max = f;
        }

        // update values
        for (int i = 0; i < data.length; i++) {
            float x = data[i];
            data[i] = (x - min) / (max - min);
        }

        return data;
    }
}