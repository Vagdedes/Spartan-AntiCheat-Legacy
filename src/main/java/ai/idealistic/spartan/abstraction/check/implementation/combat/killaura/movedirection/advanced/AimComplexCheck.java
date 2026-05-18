package ai.idealistic.spartan.abstraction.check.implementation.combat.killaura.movedirection.advanced;

import ai.idealistic.spartan.abstraction.check.CheckDetection;
import ai.idealistic.spartan.abstraction.event.RotationEvent;
import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import kireiko.dev.millennium.math.Statistics;
import kireiko.dev.millennium.types.Vec2;
import kireiko.dev.millennium.types.Vec2f;
import kireiko.dev.millennium.types.Vec2i;

import java.util.ArrayList;
import java.util.List;

public final class AimComplexCheck implements HeuristicComponent {

    private static final int ENTROPY_VL_LIMIT = 30;
    private static final float DISTINCT_VL_LIMIT = 4.0f;
    private static final float RANDOMIZER_VL_LIMIT = 2.5f;
    private static final int ROTATION_BUFFER_SIZE = 10;
    private static final int KIREIKO_BUFFER_SIZE = 7;

    private final CheckDetection detection;
    private final PlayerProtocol profile;

    private final List<Vec2i> normalizedRotations = new ArrayList<>();
    private final List<Vec2f> rawRotations = new ArrayList<>();
    private final List<Vec2> kireikoGeneric = new ArrayList<>();

    private double oldShannonYaw = 0;
    private double oldShannonPitch = 0;

    private float entropyPerfectBuffer = 0f;
    private float entropySimilarBuffer = 0f;
    private float distinctBuffer = 0f;
    private float randomizerBuffer = 0f;
    private float machineHeartBuffer = 0f;

    public AimComplexCheck(final CheckDetection detection, final PlayerProtocol protocol) {
        this.detection = detection;
        this.profile = protocol;
    }

    @Override
    public void process(final RotationEvent rotationUpdate) {
        this.detection.call(
                () -> {
                    Vec2f delta = new Vec2f(rotationUpdate.getAbsDelta().getX(), rotationUpdate.getAbsDelta().getY());
                    if (delta.getY() == 0 && delta.getX() == 0) return;
                    this.rawRotations.add(delta);

                    double gcdValue = Statistics.getGCDValue(0.5d) * 3;
                    this.normalizedRotations.add(new Vec2i(
                            (int) (delta.getX() / gcdValue),
                            (int) (delta.getY() / gcdValue)
                    ));

                    if (this.normalizedRotations.size() >= ROTATION_BUFFER_SIZE) {
                        this.checkSpikes();
                    }
                    if (this.rawRotations.size() >= ROTATION_BUFFER_SIZE) {
                        this.checkRaw();
                    }
                },
                () -> {
                    normalizedRotations.clear();
                    rawRotations.clear();
                    kireikoGeneric.clear();
                }
        );
    }

    private void checkRaw() {
        final int sens = profile.calculateSensitivity();
        final int sensTemp = profile.getSensitivityProcessor().totalSensitivityClient;

        final List<Float> x = new ArrayList<>();
        final List<Float> y = new ArrayList<>();
        for (Vec2f vec2 : this.rawRotations) {
            x.add(vec2.getX());
            y.add(vec2.getY());
        }

        final int disX = Statistics.getDistinct(x);
        final double shannonYaw = Statistics.getShannonEntropy(x);
        final double shannonPitch = Statistics.getShannonEntropy(y);
        final boolean valid = sens >= 60 && sens <= 150 && sensTemp >= 60 && sensTemp < 150;

        if (valid && getDifference(shannonYaw, oldShannonYaw) < 1e-5 && getDifference(shannonPitch, oldShannonPitch) < 1e-5) {
            entropyPerfectBuffer = Math.min(200, entropyPerfectBuffer + 1.0f);
            if (entropyPerfectBuffer > ENTROPY_VL_LIMIT) {
                detection.call(() -> detection.cancel("Analysis (Perfect Entropy)", profile.getFromLocation()));
                entropyPerfectBuffer = ENTROPY_VL_LIMIT - 1;
            }
        } else {
            entropyPerfectBuffer = Math.max(0, entropyPerfectBuffer - 2.0f);
        }

        if (valid && getDifference(shannonYaw, shannonPitch) < 1e-5) {
            entropySimilarBuffer = Math.min(200, entropySimilarBuffer + 1.0f);
            if (entropySimilarBuffer > ENTROPY_VL_LIMIT) {
                detection.call(() -> detection.cancel("Analysis (Similar Entropy)", profile.getFromLocation()));
                entropySimilarBuffer = ENTROPY_VL_LIMIT - 1;
            }
        } else {
            entropySimilarBuffer = Math.max(0, entropySimilarBuffer - 2.0f);
        }

        if (disX < 8 && Math.abs(Statistics.getAverage(x)) > 2.5) {
            distinctBuffer = Math.min(200, distinctBuffer + 1.7f);
            if (distinctBuffer >= DISTINCT_VL_LIMIT) {
                detection.call(() -> detection.cancel("Flaw (Invalid Distinct)", profile.getFromLocation()));
                distinctBuffer = Math.max(0, distinctBuffer - 0.5f);
            }
        } else {
            distinctBuffer = Math.max(0, distinctBuffer - 0.35f);
        }

        this.oldShannonYaw = shannonYaw;
        this.oldShannonPitch = shannonPitch;
        this.rawRotations.clear();
    }

    private void checkSpikes() {
        List<Integer> gcdYaw = new ArrayList<>();
        List<Integer> gcdPitch = new ArrayList<>();
        for (Vec2i vec2i : this.normalizedRotations) {
            gcdYaw.add(vec2i.getX());
            gcdPitch.add(vec2i.getY());
        }
        this.normalizedRotations.clear();
        if (gcdYaw.isEmpty()) return;

        Vec2 kireikoGenericVec = new Vec2(Statistics.getKireikoGeneric(gcdYaw), Statistics.getKireikoGeneric(gcdPitch));
        this.kireikoGeneric.add(kireikoGenericVec);

        if (this.kireikoGeneric.size() >= KIREIKO_BUFFER_SIZE) {
            final List<Double> x = new ArrayList<>();
            for (Vec2 vec2 : this.kireikoGeneric) {
                x.add(vec2.getX());
            }

            double xDev = Statistics.getStandardDeviation(x);
            double xMax = Statistics.getMax(x);

            if (xDev > 5 && xDev < 22 && xMax < 50) {
                machineHeartBuffer += (Statistics.getAverage(x) < 6.0) ? 0 : (xDev < 10) ? 1.5f : 1.0f;
                if (machineHeartBuffer >= 7.0f) {
                    machineHeartBuffer = 6.0f;
                }
            } else {
                machineHeartBuffer -= (xDev < 40 || xMax < 70) ? -0.4f : -0.8f;
            }
            machineHeartBuffer = Math.max(0, machineHeartBuffer);
            this.kireikoGeneric.clear();
        }

        double devX = Statistics.getVariance(gcdYaw);
        double devY = Statistics.getVariance(gcdPitch);
        double min = Math.min(devX, devY);
        double max = Math.max(devX, devY);

        if ((min < 0.09 && max > 35 && Statistics.getMin(gcdPitch) != 0.0) && profile.calculateSensitivity() > 50) {
            randomizerBuffer = Math.min(200, randomizerBuffer + 1.0f);
            if (randomizerBuffer > RANDOMIZER_VL_LIMIT) {
                detection.call(() -> detection.cancel("Analysis (Randomizer Flaw)", profile.getFromLocation()));
                randomizerBuffer = RANDOMIZER_VL_LIMIT - 1;
            }
        } else {
            randomizerBuffer = Math.max(0, randomizerBuffer - 0.4f);
        }
    }

    private static double getDifference(double a, double b) {
        return Math.abs(Math.abs(a) - Math.abs(b));
    }
}