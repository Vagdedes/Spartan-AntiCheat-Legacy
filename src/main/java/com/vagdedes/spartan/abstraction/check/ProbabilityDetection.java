package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.PlayerEvidence;
import com.vagdedes.spartan.functionality.tracking.ResearchEngine;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ProbabilityDetection extends CheckDetection {

    private int lastProbability;
    private final int[] dataCount;
    private final long[] firstTime, lastTime;
    private final Map<Check.DataType, Collection<Long>> data;
    private final Object synchronizer = new Object();

    public ProbabilityDetection(CheckRunner executor, String name, boolean def) {
        super(executor, name, def);
        this.data = new ConcurrentHashMap<>(Check.DataType.values().length);
        this.dataCount = new int[Check.DataType.values().length];
        this.firstTime = new long[Check.DataType.values().length];
        this.lastTime = new long[Check.DataType.values().length];

        for (Check.DataType dataType : Check.DataType.values()) {
            this.firstTime[dataType.ordinal()] = -1L;
            this.lastTime[dataType.ordinal()] = -1L;
            this.dataCount[dataType.ordinal()] = 0;
        }
    }

    // Data

    @Override
    protected final boolean hasData(PlayerProfile profile, Check.DataType dataType) {
        return getFirstTime(dataType) != null;
    }

    @Override
    protected final boolean hasSufficientData(Check.DataType dataType) {
        Collection<PlayerProfile> playerProfiles = ResearchEngine.getPlayerProfiles();
        int total = playerProfiles.size();

        if (total >= PlayerEvidence.factorRequirement) {
            int pass = 0,
                    count = 0;
            int halfRequirement = AlgebraUtils.integerCeil(PlayerEvidence.factorRequirement / 2.0);

            for (PlayerProfile profile : playerProfiles) {
                int individualCount = profile.getRunner(this.hackType).getDetection(this.name).getDataCount(dataType);

                if (individualCount > 0) {
                    count += individualCount;
                    pass++;

                    if (pass == PlayerEvidence.factorRequirement) {
                        return true;
                    } else if (pass >= halfRequirement
                            && (count / (double) total) > PlayerEvidence.factorRequirement) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void clearData(Check.DataType dataType) {
        this.data.remove(dataType);
        this.dataCount[dataType.ordinal()] = 0;
        this.firstTime[dataType.ordinal()] = -1L;
        this.lastTime[dataType.ordinal()] = -1L;
    }

    @Override
    public final void store(Check.DataType dataType, long time) {
        Collection<Long> data = this.data.computeIfAbsent(
                dataType,
                k -> new TreeSet<>()
        );

        synchronized (this.synchronizer) {
            if (data.size() == 2_048) {
                Iterator<Long> iterator = data.iterator();
                iterator.next();
                iterator.remove();
            }
            data.add(time);

            if (this.firstTime[dataType.ordinal()] == -1L) {
                this.firstTime[dataType.ordinal()] = time;
            } else if (time < this.firstTime[dataType.ordinal()]) {
                this.firstTime[dataType.ordinal()] = time;
            }
            this.lastTime[dataType.ordinal()] = time;
            this.dataCount[dataType.ordinal()]++;
        }
    }

    @Override
    public final double getData(PlayerProfile profile, Check.DataType dataType) {
        Collection<Long> data = this.data.get(dataType);

        if (data == null) {
            return CheckDetection.defaultData();
        } else {
            double violationCount,
                    averageViolationVelocity,
                    comparisonCount = 0.0;
            long violationVelocitySquareSum = 0;

            synchronized (this.synchronizer) {
                violationCount = data.size();
                Iterator<Long> iterator = data.iterator();

                if (iterator.hasNext()) {
                    long previous = iterator.next();

                    while (iterator.hasNext()) {
                        long current = iterator.next();

                        if (profile.getContinuity().wasOnline(current, previous)) {
                            comparisonCount++;
                            long difference = current - previous;
                            previous = current;
                            violationVelocitySquareSum += difference * difference;
                        }
                    }
                }
            }
            long onlineTime = profile.getContinuity().getOnlineTime();

            if (onlineTime > 0L) {
                onlineTime /= 1_000L; // Convert to seconds
                violationCount /= (double) onlineTime;
            }
            if (comparisonCount == 0.0) {
                averageViolationVelocity = Long.MAX_VALUE;
            } else {
                averageViolationVelocity = Math.sqrt(violationVelocitySquareSum / comparisonCount);
            }
            double result = averageViolationVelocity / violationCount;

            if (result == Double.POSITIVE_INFINITY
                    || result == Double.NEGATIVE_INFINITY) {
                return CheckDetection.defaultData();
            } else {
                CheckDetection.setDefaultData(result);
                return result;
            }
        }
    }

    @Override
    protected final int getDataCount(Check.DataType dataType) {
        return this.dataCount[dataType.ordinal()];
    }

    @Override
    final Long getFirstTime(Check.DataType dataType) {
        return this.firstTime[dataType.ordinal()];
    }

    @Override
    final Long getLastTime(Check.DataType dataType) {
        return this.lastTime[dataType.ordinal()];
    }

    @Override
    final double getDataCompletion(Check.DataType dataType) {
        Collection<PlayerProfile> playerProfiles = ResearchEngine.getPlayerProfiles();

        if (!playerProfiles.isEmpty()) {
            int count = 0;

            for (PlayerProfile profile : playerProfiles) {
                if (profile.getRunner(this.hackType).getDetection(this.name).hasData(profile, dataType)) {
                    count++;

                    if (count == PlayerEvidence.factorRequirement) {
                        return 1.0;
                    }
                }
            }
            return count / (double) PlayerEvidence.factorRequirement;
        } else {
            return 0.0;
        }
    }

    // Notification

    @Override
    public final boolean canSendNotification(Object detected, int probability) {
        long time = System.currentTimeMillis();
        SpartanProtocol protocol = detected instanceof SpartanProtocol
                ? (SpartanProtocol) detected
                : null;

        if (this.notifications <= time
                && (this.lastProbability != probability
                || time - this.notifications >= 5_000L
                || (protocol != null
                ? protocol.spartan.equals(this.protocol.spartan)
                : this.protocol.bukkit().getName().equals(detected.toString())))) {
            int ticks = this.executor.getNotificationTicksCooldown(protocol);

            if (ticks > 0) {
                this.notifications = time + (ticks * TPS.tickTime);
            } else {
                this.notifications = time;
            }
            this.lastProbability = probability;
            return true;
        }
        return false;
    }

}
