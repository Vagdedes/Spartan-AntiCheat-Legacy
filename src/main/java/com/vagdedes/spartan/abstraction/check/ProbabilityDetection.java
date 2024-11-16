package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.PlayerEvidence;
import com.vagdedes.spartan.functionality.tracking.ResearchEngine;

import java.util.*;

public abstract class ProbabilityDetection extends CheckDetection {

    private int lastProbability;
    private final Map<Check.DataType, Collection<Long>> data;

    public ProbabilityDetection(CheckRunner executor, String name, boolean def) {
        super(executor, name, def);
        this.data = Collections.synchronizedMap(
                new LinkedHashMap<>(Check.DataType.values().length)
        );
    }

    // Data

    @Override
    protected final boolean hasData(Check.DataType dataType) {
        Collection<Long> data;

        synchronized (this.data) {
            data = this.data.get(dataType);
        }
        return data != null && !data.isEmpty();
    }

    @Override
    public void clearData(Check.DataType dataType) {
        synchronized (this.data) {
            this.data.remove(dataType);
        }
    }

    @Override
    public final void store(Check.DataType dataType, long time) {
        synchronized (this.data) {
            Collection<Long> data = this.data.computeIfAbsent(
                    dataType,
                    k -> new ArrayList<>()
            );

            if (data.size() == 2_048) {
                Iterator<Long> iterator = data.iterator();
                iterator.next();
                iterator.remove();
            }
            data.add(time);
        }
        ResearchEngine.queueToCache(this.hackType);
    }

    @Override
    public final Double getTimeDifference(Check.DataType dataType) {
        if (this.hasData(dataType)) {
            long timeLimit = 60_000L;

            if (this.data.size() == 1) {
                synchronized (this.data) {
                    //return timeLimit / this.data.values().iterator().next().iterator().next();
                    return 0.0;
                }
            } else {
                double squareSum = 0.0;
                int size;

                synchronized (this.data) {
                    Collection<Long> data = this.data.get(dataType);
                    size = data.size();
                    Iterator<Long> iterator = data.iterator();
                    long previous = iterator.next();

                    while (iterator.hasNext()) {
                        Long current = iterator.next();
                        double difference = Math.min(
                                current - previous,
                                timeLimit
                        );
                        previous = current;
                        squareSum += difference * difference;
                    }
                }
                return Math.sqrt(squareSum / (size - 1.0));
            }
        } else {
            return null;
        }
    }

    @Override
    public Long getFirstTime(Check.DataType dataType) {
        Long time;

        synchronized (this.data) {
            Collection<Long> data = this.data.get(dataType);

            if (data != null) {
                time = data.iterator().next();
            } else {
                time = null;
            }
        }
        return time;
    }

    @Override
    public Long getLastTime(Check.DataType dataType) {
        Long time;

        synchronized (this.data) {
            Collection<Long> data = this.data.get(dataType);

            if (data != null) {
                time = data.stream().reduce((first, second) -> second).orElse(null);
            } else {
                time = null;
            }
        }
        return time;
    }

    @Override
    public final boolean canSendNotification(Object detected, int probability) {
        long time = System.currentTimeMillis();

        if (this.notifications <= time
                && (this.lastProbability != probability
                || time - this.notifications >= 5_000L)) {
            boolean player = detected instanceof SpartanProtocol;
            int ticks = this.executor.getNotificationTicksCooldown(
                    player ? (SpartanProtocol) detected : null
            );

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

    @Override
    public final double getDataCompletion(Check.DataType dataType) {
        List<PlayerProfile> playerProfiles = ResearchEngine.getPlayerProfiles();

        if (!playerProfiles.isEmpty()) {
            int requirement = PlayerEvidence.probabilityToFactors(PlayerEvidence.punishmentProbability);
            int count = 0;

            for (PlayerProfile profile : playerProfiles) {
                if (profile.getRunner(this.hackType).getDetection(this.name).hasData(dataType)) {
                    count++;

                    if (count == requirement) {
                        return 1.0;
                    }
                }
            }
            return count / (double) requirement;
        } else {
            return 0.0;
        }
    }

    // Notification

    @Override
    protected final boolean hasSufficientData(Check.DataType dataType) {
        List<PlayerProfile> playerProfiles = ResearchEngine.getPlayerProfiles();
        int requirement = PlayerEvidence.probabilityToFactors(PlayerEvidence.punishmentProbability);

        if (playerProfiles.size() >= requirement) {
            int count = 0;

            for (PlayerProfile profile : playerProfiles) {
                if (profile.getRunner(this.hackType).getDetection(this.name).hasData(dataType)) {
                    count++;

                    if (count == requirement) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
