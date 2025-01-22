package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.tracking.PlayerEvidence;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class ProbabilityDetection extends CheckDetection {

    private final List<Long>[] data;

    public ProbabilityDetection(
            CheckRunner executor,
            Check.DataType forcedDataType,
            Check.DetectionType detectionType,
            String name,
            Boolean def
    ) {
        super(executor, forcedDataType, detectionType, name, def);
        this.data = new List[Check.DataType.values().length];

        for (Check.DataType dataType : Check.DataType.values()) {
            this.data[dataType.ordinal()] = new CopyOnWriteArrayList<>();
        }
    }

    // Probability

    @Override
    public final double getProbability(Check.DataType dataType) {
        return super.getProbability(dataType);
    }

    @Override
    public final void setProbability(Check.DataType dataType, double probability) {
        super.setProbability(dataType, probability);
    }

    @Override
    public final void clearProbability(Check.DataType dataType) {
        super.clearProbability(dataType);
    }

    // Data

    @Override
    protected final boolean hasSufficientData(Check.DataType dataType) {
        return this.getRawProbability(dataType) != PlayerEvidence.nullProbability;
    }

    @Override
    public final void sortData() {
        for (Check.DataType dataType : Check.DataType.values()) {
            List<Long> data = this.data[dataType.ordinal()];

            if (data != null) {
                Collections.sort(data);
            }
        }
    }

    @Override
    public void clearData(Check.DataType dataType) {
        this.data[dataType.ordinal()].clear();
    }

    @Override
    public final void storeData(Check.DataType dataType, long time) {
        Collection<Long> data = this.data[dataType.ordinal()];
        int size = data.size() - 2_048;

        if (size > 0) {
            Iterator<Long> iterator = data.iterator();

            while (iterator.hasNext() && size > 0) {
                if (data.remove(iterator.next())) {
                    size--;
                }
            }
        }
        data.add(time);
    }

    @Override
    public final double getAllData(PlayerProfile profile, Check.DataType dataType) {
        Collection<Long> data = this.data[dataType.ordinal()];

        if (data.isEmpty()) {
            return -1.0;
        }
        double onlineTime = profile.getContinuity().getOnlineTime();

        if (onlineTime == 0.0) {
            return -1.0;
        }
        double violationCount = data.size(),
                comparisonCount = 0.0;
        long violationVelocitySquareSum = 0;
        Iterator<Long> iterator = data.iterator();

        if (iterator.hasNext()) {
            long previous = iterator.next();

            while (iterator.hasNext()) {
                long current = iterator.next();

                if (profile.getContinuity().wasOnline(current, previous)) {
                    comparisonCount++;
                    long delta = current - previous;
                    violationVelocitySquareSum += delta * delta;
                }
                previous = current;
            }
        }
        if (comparisonCount == 0.0) {
            return -1.0;
        }
        violationCount /= onlineTime / 1000.0; // Convert online time to seconds

        double averageViolationVelocity = Math.sqrt(violationVelocitySquareSum / comparisonCount),
                result = averageViolationVelocity / violationCount;

        if (Double.isInfinite(result)
                || Double.isNaN(result)) {
            return -1.0;
        }
        return result;
    }

    @Override
    public final List<Double> getDataSamples(PlayerProfile profile, Check.DataType dataType) {
        Collection<Long> data = this.data[dataType.ordinal()];

        if (data.isEmpty()) {
            return new ArrayList<>(0);
        }
        if (!profile.getContinuity().hasOnlineTime()) {
            return new ArrayList<>(0);
        }
        List<Double> samples = new ArrayList<>();
        double violationCount = 0.0,
                comparisonCount = 0.0;
        long violationVelocitySquareSum = 0;
        Iterator<Long> iterator = data.iterator();

        if (iterator.hasNext()) {
            long previous = iterator.next(),
                    firstTime = -1L;

            while (iterator.hasNext()) {
                long current = iterator.next();

                if (profile.getContinuity().wasOnline(current, previous)) {
                    if (firstTime == -1L) {
                        firstTime = previous;
                        violationCount = 2; // Current & previous
                    } else {
                        violationCount++;
                    }
                    comparisonCount++;
                    long delta = current - previous;
                    violationVelocitySquareSum += delta * delta;
                } else if (firstTime != -1L) {
                    // Calculate
                    violationCount /= (previous - firstTime) / 1000.0; // Convert online time to seconds
                    double averageViolationVelocity = Math.sqrt(violationVelocitySquareSum / comparisonCount),
                            result = averageViolationVelocity / violationCount;

                    // Queue
                    if (!Double.isInfinite(result)
                            && !Double.isNaN(result)) {
                        samples.add(result);
                    }

                    // Reset
                    comparisonCount = 0.0;
                    violationVelocitySquareSum = 0;
                    firstTime = -1L;
                }
                previous = current;
            }
        }
        return samples;
    }

}
