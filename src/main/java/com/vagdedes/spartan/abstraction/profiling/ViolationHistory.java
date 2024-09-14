package com.vagdedes.spartan.abstraction.profiling;

import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import me.vagdedes.spartan.system.Enums;

import java.util.*;

public class ViolationHistory {

    private final Map<Long, PlayerViolation> times;

    ViolationHistory() {
        this.times = Collections.synchronizedMap(new TreeMap<>());
    }

    public boolean isEmpty() {
        return this.times.size() > 1;
    }

    public void store(Enums.HackType hackType, PlayerViolation playerViolation) {
        synchronized (this.times) {
            this.times.put(playerViolation.time, playerViolation);
        }
        ResearchEngine.queueToCache(hackType);
    }

    public Double getTimeDifference(Enums.HackType hackType) {
        int size = this.times.size();

        if (size > 1) {
            Map<Integer, double[]> squareSum = new LinkedHashMap<>();

            synchronized (this.times) {
                Iterator<PlayerViolation> iterator = this.times.values().iterator();
                long previous = iterator.next().time;

                while (iterator.hasNext()) {
                    PlayerViolation violation = iterator.next();
                    double difference = Math.min(
                            violation.time - previous,
                            60_000L
                    );
                    previous = violation.time;
                    difference /= violation.increase + (hackType.violationTimeWorth / difference);
                    double[] data = squareSum.getOrDefault(violation.hash, new double[]{0.0, 0.0});
                    data[0] += difference * difference;
                    data[1] += 1.0;
                    squareSum.put(violation.hash, data);
                }
            }
            double finalSquareSum = 0.0;

            for (Map.Entry<Integer, double[]> entry : squareSum.entrySet()) {
                double value = Math.sqrt(entry.getValue()[0] / entry.getValue()[1]);
                finalSquareSum += value * value;
            }
            return Math.sqrt(finalSquareSum / (size - 1.0));
        } else {
            return null;
        }
    }

}
