package ai.idealistic.spartan.abstraction.check.implementation.combat.killaura.movedirection.advanced;

import ai.idealistic.spartan.abstraction.check.CheckDetection;
import ai.idealistic.spartan.abstraction.event.RotationEvent;
import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import kireiko.dev.millennium.math.Statistics;
import kireiko.dev.millennium.types.Vec2f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public final class AimStatisticsCheck implements HeuristicComponent {
    private static final int ROTATION_BUFFER_SIZE = 25;
    private static final float IQR_VL_LIMIT = 11.0f;
    private static final float ZFACTOR_VL_LIMIT = 7.0f;
    private static final float IMPROBABLE_VL_LIMIT = 15.0f;
    
    private final CheckDetection detection;
    private final PlayerProtocol profile;
    
    private final List<Vec2f> rotationDeltas = new ArrayList<>();
    private final List<Double> shannonAnalysis = new ArrayList<>();

    private float iqrBuffer = 0f;
    private float zFactorBuffer = 0f;
    private float improbableBuffer = 0f;

    public AimStatisticsCheck(final CheckDetection detection, final PlayerProtocol protocol) {
        this.detection = detection;
        this.profile = protocol;
    }

    @Override
    public void process(final RotationEvent rotationUpdate) {
        if (rotationUpdate.getAbsDelta().getY() == 0 && rotationUpdate.getAbsDelta().getX() == 0) return;

        final float deltaYaw = rotationUpdate.getAbsDelta().getX();
        final float deltaPitch = rotationUpdate.getAbsDelta().getY();

        rotationDeltas.add(new Vec2f(deltaYaw, deltaPitch));

        if (rotationDeltas.size() >= ROTATION_BUFFER_SIZE) {
            runStatisticalAnalysis();
            rotationDeltas.clear();
        }
    }

    private void runStatisticalAnalysis() {
        float total = 0;
        
        // Разделяем дельты на списки по осям X и Y
        List<Float> x = new ArrayList<>();
        List<Float> y = new ArrayList<>();
        for (Vec2f vec2 : this.rotationDeltas) {
            x.add(vec2.getX());
            y.add(vec2.getY());
        }

        final List<Float> jiffYaw = Statistics.getJiffDelta(x, 5);
        final List<Float> jiffPitch = Statistics.getJiffDelta(y, 5);
        final List<Float> jiffOmni = new ArrayList<>();
        for (int i = 0; i < Math.min(jiffYaw.size(), jiffPitch.size()); i++) {
            jiffOmni.add(jiffYaw.get(i) / jiffPitch.get(i));
        }

        int infs = 0;
        for (float j : jiffOmni) if (Float.isInfinite(j)) infs++;
        
        final double iqr = Statistics.getIQR(jiffOmni);
        if (iqr > 12.5 && iqr < 96 && infs > 0) {
            iqrBuffer += (iqr > 20) ? 1.4f : 0.8f;
            if (iqrBuffer > IQR_VL_LIMIT) {
                detection.cancel("Statistics (IQR)", profile.getFromLocation());
                iqrBuffer = IQR_VL_LIMIT - 2f;
            }
        } else if (iqr < 13 || infs == 0) {
            iqrBuffer -= (iqr < 7) ? 5.0f : 3.5f;
        }

        int jiffPatterns = 0;
        for (int i = 0; i < jiffYaw.size(); i++) {
            float f = jiffYaw.get(i);
            if (!String.valueOf(f).contains("E") || f == 0) continue;
            for (int r = 0; r < jiffYaw.size(); r++) {
                if (r == i) continue;
                if (f == jiffYaw.get(r)) jiffPatterns++;
            }
        }
        if (jiffPatterns > 2 && Statistics.getAverage(x) > 3.0 && jiffPatterns != 6 && jiffPatterns != 12 && jiffPatterns != 4) {
             detection.cancel("Statistics (Bot Pattern " + jiffPatterns + ")", profile.getFromLocation());
        }

        final List<Double> zFactorYaw = Statistics.getZScoreOutliers(x, 2.0);
        boolean positive = false, negative = false;
        for (double d : zFactorYaw) {
            if (d > 10) positive = true;
            if (d < -10) negative = true;
        }

        if (zFactorYaw.size() == 2 && positive && negative && Statistics.getMax(zFactorYaw) < 55) {
            zFactorBuffer += 1.5f;
            if (zFactorBuffer > 4) total++;
            if (zFactorBuffer > ZFACTOR_VL_LIMIT) {
                detection.cancel("Statistics (Suspicious zFactor)", profile.getFromLocation());
                zFactorBuffer = ZFACTOR_VL_LIMIT - 1f;
            }
        } else {
            zFactorBuffer -= 1.2f;
        }

        final double kTest = Statistics.kolmogorovSmirnovTest(Statistics.getJiffDelta(x, 6), Function.identity());
        if (kTest > 10 && Math.abs(Statistics.getAverage(x)) < 13) {
            total++;
        }

        if (total < 2.0) {
            improbableBuffer -= 2f;
        } else if (total > 2.0) {
            improbableBuffer += 5f;
            if (improbableBuffer >= IMPROBABLE_VL_LIMIT) {
                detection.cancel("Statistics (Improbable)", profile.getFromLocation());
                improbableBuffer = IMPROBABLE_VL_LIMIT - 2.0f;
            }
        }

        iqrBuffer = Math.max(0, iqrBuffer);
        zFactorBuffer = Math.max(0, zFactorBuffer);
        improbableBuffer = Math.max(0, improbableBuffer);
    }
}