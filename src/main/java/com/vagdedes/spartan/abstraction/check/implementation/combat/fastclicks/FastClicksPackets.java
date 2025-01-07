package com.vagdedes.spartan.abstraction.check.implementation.combat.fastclicks;

import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.ProbabilityDetection;
import com.vagdedes.spartan.abstraction.check.definition.ImplementedProbabilityDetection;
import com.vagdedes.spartan.abstraction.event.PlayerTickEvent;
import com.vagdedes.spartan.utils.math.statistics.StatisticsMath;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FastClicksPackets extends ProbabilityDetection {

    private long lastTickTime = System.currentTimeMillis();
    private final List<Integer> buffer = new ArrayList<>();

    {
        for (int i = 0; i < 7; i++) buffer.add(0);
    }

    private final CheckDetection
            floorTempo,
            deviation,
            skewness,
            kurtosis,
            variance;
    private final List<Long>
            samples = new CopyOnWriteArrayList<>(),
            samplesMassive = new CopyOnWriteArrayList<>();

    FastClicksPackets(CheckRunner executor) {
        super(executor, null, true);
        this.floorTempo = new ImplementedProbabilityDetection(this, "floor_tempo", true);
        this.deviation = new ImplementedProbabilityDetection(this, "deviation", true);
        this.skewness = new ImplementedProbabilityDetection(this, "skewness", true);
        this.kurtosis = new ImplementedProbabilityDetection(this, "kurtosis", true);
        this.variance = new ImplementedProbabilityDetection(this, "variance", true);
    }

    void run(long delay) {
        if (!tickingCorrect()) return;
        long t = System.currentTimeMillis();
        this.samples.add(delay);
        this.samplesMassive.add(delay);
        if (this.samples.size() >= 20) {
            { // logic
                final double cps = StatisticsMath.getCps(this.samples) * 50;
                final double limit = hackType.getCheck().getNumericalOption(
                        "clicks_per_second_limit",
                        15
                );
                final double variance = StatisticsMath.getVariance(this.samples);
                final double kurtosis = StatisticsMath.getKurtosis(this.samples);
                if (cps > limit) this.cancel("type: limit ("
                        + (int) cps + "/" + (int) limit + ")");
                { // floor
                    final double floor = Math.abs(Math.floor(cps) - cps);
                    if (floor < 0.07) {
                        if (addToBuffer(1, 2) > 3) {
                            this.floorTempo.cancel("type: floor tempo (" + floor + ")");
                            addToBuffer(1, -1);
                        }
                    } else {
                        addToBuffer(1, -1);
                    }
                }
                { // kurtosis
                    if (kurtosis < 45000) {
                        if (addToBuffer(4, 3) > 6) {
                            this.kurtosis.cancel("type: kurtosis (" + (int) kurtosis + ")");
                            addToBuffer(4, -1);
                        }
                    } else {
                        addToBuffer(4, -2);
                    }
                }
                { // variance
                    if (variance < 1100 && variance > 200 && cps > 7) {
                        if (addToBuffer(5, 3) > 6) {
                            this.variance.cancel("type: variance (" + (int) variance + ")");
                            addToBuffer(5, -1);
                        }
                    } else {
                        addToBuffer(5, -3);
                    }
                }
            }
            this.samples.clear();
        }
        if (this.samplesMassive.size() >= 50) {
            { // logic
                final double standardDev = StatisticsMath.getStandardDeviation(this.samplesMassive);
                final double skewness = StatisticsMath.getSkewness(this.samplesMassive);
                if (standardDev < 50) {
                    if (addToBuffer(2, 2) > 3) {
                        this.deviation.cancel("type: low deviation (" + standardDev + ")");
                        addToBuffer(2, -1);
                    }
                } else {
                    addToBuffer(2, -1);
                }
                if (skewness < -0.01) {
                    if (addToBuffer(3, 2) > 3) {
                        this.skewness.cancel("type: invalid skewness (" + skewness + ")");
                        addToBuffer(3, -1);
                    }
                } else {
                    addToBuffer(3, -2);
                }
            }
            this.samplesMassive.clear();
        }
    }

    void tick(PlayerTickEvent event) {
        this.lastTickTime = event.time;
    }

    private int addToBuffer(int i, int v) {
        this.buffer.set(i, Math.max(this.buffer.get(i) + v, 0));
        return this.buffer.get(i);
    }

    private boolean tickingCorrect() {
        return (System.currentTimeMillis() - this.lastTickTime) < 70;
    }

}
