package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.protocol.SpartanPlayer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.PlayerEvidence;
import com.vagdedes.spartan.functionality.tracking.ResearchEngine;

import java.util.*;

public abstract class ProbabilityDetectionExecutor extends CheckDetection {

    private int lastProbability;
    private final Map<Check.DataType, Map<Long, Double>> data;

    public ProbabilityDetectionExecutor(CheckProcess executor, String name, boolean def) {
        super(executor, name, def);
        this.data = Collections.synchronizedMap(
                new LinkedHashMap<>(Check.DataType.values().length)
        );
    }

    public ProbabilityDetectionExecutor(CheckProcess executor, String name, boolean def) {
        super(executor, name, def);
        this.data = Collections.synchronizedMap(
                new LinkedHashMap<>(Check.DataType.values().length)
        );
    }

    @Override
    protected final boolean hasDataToCompare(Check.DataType dataType) {
        Map<Long, Double> map = this.data.get(dataType);
        return map != null && !map.isEmpty();
    }

    @Override
    public final void store(Check.DataType dataType, long time, double increase) {
        synchronized (this.data) {
            Map<Long, Double> map = this.data.computeIfAbsent(
                    dataType,
                    k -> new TreeMap<>()
            );

            if (map.size() == 2_048) {
                Iterator<Long> iterator = map.keySet().iterator();
                iterator.next();
                iterator.remove();
            }
            map.put(time, increase);
        }
        ResearchEngine.queueToCache(this.hackType);
    }

    @Override
    public final Double getTimeDifference(Check.DataType dataType) {
        if (this.hasDataToCompare(dataType)) {
            if (this.data.size() == 1) {
                synchronized (this.data) {
                return this.data.values().iterator().next().values().iterator().next();
                }
            } else {
                double squareSum = 0.0;
                int size;

                synchronized (this.data) {
                    Map<Long, Double> map = this.data.get(dataType);
                    size = map.size();
                    Iterator<Map.Entry<Long, Double>> iterator = map.entrySet().iterator();
                    long previous = iterator.next().getKey();

                    while (iterator.hasNext()) {
                        Map.Entry<Long, Double> entry = iterator.next();
                        double difference = Math.min(
                                (entry.getKey() - previous) / entry.getValue(),
                                60_000L
                        );
                        previous = entry.getKey();
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
    public final boolean canSendNotification(Object detected, int probability) {
        long time = System.currentTimeMillis();

        if (this.notifications <= time
                && (this.lastProbability != probability
                || time - this.notifications >= 5_000L)) {
            boolean player = detected instanceof SpartanPlayer;
            int ticks = this.executor.getNotificationTicksCooldown(
                    player ? (SpartanProtocol) detected : null
            );

            if (ticks > 0) {
                boolean access;

                if (player) {
                    access = ((SpartanPlayer) detected).getExecutor(
                            hackType
                    ).getDetection(
                            this.name
                    ).surpassedProbability(
                            ((SpartanPlayer) detected).dataType,
                            PlayerEvidence.notificationProbability
                    );
                } else {
                    PlayerProfile profile = ResearchEngine.getPlayerProfile(detected.toString(), true);
                    access = profile.getExecutor(
                            hackType
                    ).getDetection(
                            this.name
                    ).surpassedProbability(
                            profile.getLastDataType(),
                            PlayerEvidence.notificationProbability
                    );
                }

                if (access) {
                    this.notifications = time + (ticks * TPS.tickTime);
                    this.lastProbability = probability;
                    return true;
                }
            } else {
                this.notifications = time;
                this.lastProbability = probability;
                return true;
            }
        }
        return false;
    }

}
