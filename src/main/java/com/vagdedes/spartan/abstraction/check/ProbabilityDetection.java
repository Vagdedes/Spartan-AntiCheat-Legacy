package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.PlayerEvidence;
import com.vagdedes.spartan.functionality.tracking.ResearchEngine;

import java.util.*;

public abstract class ProbabilityDetection extends CheckDetection {

    private int lastProbability;
    private final Long[] lastTime;
    private final Map<Check.DataType, Collection<Long>> data;
    private final Map<Check.DataType, Map<Long, Boolean>> continuity;

    public ProbabilityDetection(CheckRunner executor, String name, boolean def) {
        super(executor, name, def);
        this.data = Collections.synchronizedMap(
                new LinkedHashMap<>(Check.DataType.values().length)
        );
        this.continuity = Collections.synchronizedMap(
                new LinkedHashMap<>(Check.DataType.values().length)
        );
        this.lastTime = new Long[Check.DataType.values().length];
    }

    // Sync

    @Override
    public final void addDataContinuation(Check.DataType dataType, long time, boolean trueFalse) {
        synchronized (this.continuity) {
            this.continuity.computeIfAbsent(
                    dataType,
                    k -> new TreeMap<>()
            ).put(time, trueFalse);
        }
    }

    private boolean wasOnline(Check.DataType dataType, long current, long previous) {
        synchronized (this.continuity) {
            Map<Long, Boolean> data = this.continuity.get(dataType);

            if (data != null) {
                long previousTime = Long.MAX_VALUE;
                Iterator<Map.Entry<Long, Boolean>> iterator = data.entrySet().iterator();

                while (iterator.hasNext()) {
                    Map.Entry<Long, Boolean> entry = iterator.next();
                    long currentTime = entry.getKey();

                    if (previous >= previousTime && current <= currentTime) {
                        if (entry.getValue()) {
                            previousTime = currentTime;
                        } else {
                            return true;
                        }
                    } else if (entry.getValue()) {
                        previousTime = currentTime;
                    } else {
                        previousTime = Long.MAX_VALUE;
                    }
                }
            }
        }
        return false;
    }

    private Long getOnlineTime(Check.DataType dataType) {
        synchronized (this.continuity) {
            Map<Long, Boolean> data = this.continuity.get(dataType);

            if (data != null) {
                boolean track = true;
                long sum = 0L, previous = -1L;

                for (Map.Entry<Long, Boolean> entry : data.entrySet()) {
                    if (entry.getValue() == track) {
                        if (track) {
                            previous = entry.getKey();
                        } else if (previous != -1L) {
                            sum += entry.getKey() - previous;
                        }
                        track = !track;
                    } else if (track) {
                        previous = entry.getKey();
                    }
                }
                return sum == 0L ? null : sum;
            }
            return null;
        }
    }

    // Data

    @Override
    protected final boolean hasData(Check.DataType dataType) {
        synchronized (this.data) {
            if (this.data.containsKey(dataType)) {
                synchronized (this.continuity) {
                    return this.continuity.containsKey(dataType);
                }
            }
        }
        return false;
    }

    @Override
    public void clearData(Check.DataType dataType) {
        synchronized (this.data) {
            this.data.remove(dataType);
            this.lastTime[dataType.ordinal()] = null;

            synchronized (this.continuity) {
                this.continuity.remove(dataType);
            }
        }
    }

    @Override
    public final void store(Check.DataType dataType, long time) {
        synchronized (this.data) {
            Collection<Long> data = this.data.computeIfAbsent(
                    dataType,
                    k -> new TreeSet<>()
            );

            if (data.size() == 2_048) {
                Iterator<Long> iterator = data.iterator();
                iterator.next();
                iterator.remove();
            }
            data.add(time);
            this.lastTime[dataType.ordinal()] = time;
        }
    }

    @Override
    public final Double getData(Check.DataType dataType) {
        if (this.hasData(dataType)) {
            double averageTime, violationsPerTime;
            Long onlineTime = this.getOnlineTime(dataType);

            synchronized (this.data) {
                Collection<Long> data = this.data.get(dataType);

                if (onlineTime != null) {
                    violationsPerTime = data.size() / (double) onlineTime;
                } else {
                    violationsPerTime = data.size();
                }
                if (data.size() == 1) {
                    averageTime = Double.MAX_VALUE;
                } else {
                    long squareSum = 0;
                    Iterator<Long> iterator = data.iterator();
                    long previous = iterator.next();
                    int subtractInvalids = 1;

                    while (iterator.hasNext()) {
                        Long current = iterator.next();

                        if (this.wasOnline(dataType, current, previous)) {
                            long difference = current - previous;
                            previous = current;
                            squareSum += difference * difference;
                        } else {
                            subtractInvalids++;
                        }
                    }
                    int division = data.size() - subtractInvalids;

                    if (division == 0) {
                        averageTime = Double.MAX_VALUE;
                    } else {
                        averageTime = Math.sqrt(squareSum / (double) division);
                    }
                }
            }
            return averageTime / violationsPerTime;
        } else {
            return null;
        }
    }

    @Override
    final Long getFirstTime(Check.DataType dataType) {
        Long time = null;

        synchronized (this.data) {
            Collection<Long> data = this.data.get(dataType);

            if (data != null) {
                time = data.iterator().next();
            }
        }
        return time;
    }

    @Override
    final Long getLastTime(Check.DataType dataType) {
        synchronized (this.data) {
            return this.lastTime[dataType.ordinal()];
        }
    }

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
                ? protocol.spartan.equals(this.protocol().spartan)
                : this.protocol.bukkit.getName().equals(detected.toString())))) {
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
