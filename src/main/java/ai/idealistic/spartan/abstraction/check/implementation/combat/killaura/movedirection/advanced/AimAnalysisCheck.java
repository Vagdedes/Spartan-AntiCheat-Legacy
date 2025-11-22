package ai.idealistic.spartan.abstraction.check.implementation.combat.killaura.movedirection.advanced;

import ai.idealistic.spartan.abstraction.check.CheckDetection;
import ai.idealistic.spartan.abstraction.event.RotationEvent;
import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import kireiko.dev.millennium.math.Statistics;
import kireiko.dev.millennium.types.Vec2f;

import java.util.ArrayList;
import java.util.List;

public final class AimAnalysisCheck implements HeuristicComponent {

    private static final float RANK_VL_LIMIT = 6.0f;
    private static final int RAW_ROTATIONS_LIMIT = 100;
    private static final int LIMITED_ROTATIONS_LIMIT = 100;
    private static final int LONG_TERM_ANALYSIS_SIZE = 10;
    private static final int SUB_STACK_SIZE = 10;

    private final CheckDetection detection;
    private final PlayerProtocol profile;
    private final List<Vec2f> rawRotations;
    private final List<Vec2f> limitedRotations;
    private final List<Float> longTermAnalysis;

    private boolean query = false;
    private float rankBuffer = 0f;

    public AimAnalysisCheck(final CheckDetection detection, final PlayerProtocol protocol) {
        this.detection = detection;
        this.profile = protocol;
        this.rawRotations = new ArrayList<>();
        this.limitedRotations = new ArrayList<>();
        this.longTermAnalysis = new ArrayList<>();
    }

    @Override
    public void process(final RotationEvent rotationUpdate) {
        this.detection.call(() -> {
            Vec2f delta = new Vec2f(rotationUpdate.getAbsDelta().getX(), rotationUpdate.getAbsDelta().getY());
            this.rawRotations.add(delta);

            if (this.rawRotations.size() >= RAW_ROTATIONS_LIMIT) {
                this.checkRaw();
            }

            if (Math.abs(delta.getX()) > 1.35 || (Math.abs(delta.getY()) > 1.35 && Math.abs(delta.getX()) > 0.32)) {
                this.limitedRotations.add(delta);
                if (this.limitedRotations.size() >= LIMITED_ROTATIONS_LIMIT) {
                    this.checkLimited();
                }
            }
        });
    }

    private void checkLimited() {
        final List<Float> x = new ArrayList<>();
        for (Vec2f vec2 : this.limitedRotations) {
            x.add(vec2.getX());
        }

        final List<Float> yawStack = new ArrayList<>();
        int resultDistinct = 0;
        for (final float yaw : x) {
            yawStack.add(yaw);
            if (yawStack.size() >= SUB_STACK_SIZE) {
                resultDistinct += Statistics.getDistinct(Statistics.getJiffDelta(yawStack, 4));
                yawStack.clear();
            }
        }

        final float distinctRank = (float) resultDistinct / 60;
        longTermAnalysis.add(distinctRank);

        if (longTermAnalysis.size() >= LONG_TERM_ANALYSIS_SIZE) {
            final double avg = Statistics.getAverage(longTermAnalysis);
            double normal = 0;
            for (double d : longTermAnalysis) {
                if (d > 0.97) normal++;
            }
            if (avg < 0.95 && normal < 4) {
                detection.call(() -> detection.cancel("Analysis (Long Term)", profile.getFromLocation()));
            }
            longTermAnalysis.clear();
        }

        this.limitedRotations.clear();
    }

    private void checkRaw() {
        final List<Float> x = new ArrayList<>();
        final List<Float> xAbs = new ArrayList<>();
        for (Vec2f vec2 : this.rawRotations) {
            x.add(vec2.getX());
            xAbs.add(vec2.getX());
        }

        final List<Float> yawStack = new ArrayList<>();
        final List<Double> resultDeviation = new ArrayList<>();
        int resultDistinct = 0;
        for (final float yaw : x) {
            yawStack.add(yaw);
            if (yawStack.size() >= SUB_STACK_SIZE) {
                resultDeviation.add(Statistics.getStandardDeviation(Statistics.getJiffDelta(yawStack, 5)));
                resultDistinct += Statistics.getDistinct(Statistics.getJiffDelta(yawStack, 4));
                yawStack.clear();
            }
        }

        final List<Double> outliers5 = Statistics.getZScoreOutliers(resultDeviation, 0.5f);
        final float distinctRank = (float) resultDistinct / 60;

        if (outliers5.isEmpty() || (outliers5.size() == 1 && Math.abs(outliers5.get(0)) > 10 && Math.abs(outliers5.get(0)) < 100)) {
            if (!query) {
                query = true;
            } else {
                detection.call(() -> detection.cancel("Analysis (Linear)", profile.getFromLocation()));
            }
        } else {
            query = false;
        }

        final boolean valid = profile.calculateSensitivity() > 20 && profile.calculateSensitivity() < 140;
        if (distinctRank < 1.0 && distinctRank > 0.7 && Statistics.getAverage(xAbs) > 1.8 && valid) {
            if (this.rankBuffer < 0.01) {
                if (distinctRank < 0.8) this.rankBuffer += 0.2f;
            } else {
                this.rankBuffer += (distinctRank > 0.9) ? 0.08f : (distinctRank > 0.8) ? 2f : 3f;
                if (this.rankBuffer >= RANK_VL_LIMIT) {
                    //detection.cancel("Analysis (Incorrect Rank " + distinctRank + ")", profile.getFromLocation());
                    this.rankBuffer = RANK_VL_LIMIT - 1;
                }
            }
        } else {
            this.rankBuffer -= 2.25f;
        }
        this.rankBuffer = Math.max(0, this.rankBuffer);

        this.rawRotations.clear();
    }
}