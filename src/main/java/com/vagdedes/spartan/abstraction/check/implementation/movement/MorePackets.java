package com.vagdedes.spartan.abstraction.check.implementation.movement;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.TPS;
import me.vagdedes.spartan.system.Enums.HackType;

import java.util.*;

public class MorePackets extends CheckExecutor {

    public static final HackType check = HackType.MorePackets;
    private final Map<Long, Collection<Double>> nmsMap;
    private final Map<Long, Double> schedulerMap;

    public MorePackets(SpartanPlayer player) {
        super(check, player);
        this.nmsMap = new LinkedHashMap<>((int) TPS.maximum + 1, 1.0f);
        this.schedulerMap = new LinkedHashMap<>((int) TPS.maximum + 1, 1.0f);
    }

    @Override
    public boolean handleInternal(boolean cancelled, Object object) {
        return false;
    }

    @Override
    public boolean runInternal(boolean cancelled) {
        if (canDo()) {
            runAsync(() -> {
                Collection<Double> moves = nmsMap.computeIfAbsent(
                        TPS.getTick(player),
                        k -> new ArrayList<>()
                );
                moves.add(player.movement.getNmsDistance());

                if (nmsMap.size() > TPS.maximum) {
                    Iterator<Long> iterator = nmsMap.keySet().iterator();
                    iterator.next();
                    iterator.remove();
                }
                if (schedulerMap.size() > TPS.maximum) {
                    Iterator<Long> iterator = schedulerMap.keySet().iterator();
                    iterator.next();
                    iterator.remove();
                }
                if (nmsMap.size() > 1) {
                    Set<Map.Entry<Long, Collection<Double>>> entries = nmsMap.entrySet();
                    double averageDistanceDifference = 0.0,
                            averageDistancePercentage = 0.0,
                            averageTicks = 0.0;

                    for (Map.Entry<Long, Collection<Double>> entry : entries) {
                        Double schedulerDistance = schedulerMap.get(entry.getKey());

                        if (schedulerDistance != null) {
                            double total = 0.0;

                            for (double nms : entry.getValue()) {
                                total += nms;
                                averageDistancePercentage += Math.abs(nms - schedulerDistance) / schedulerDistance;
                            }
                            averageTicks += entry.getValue().size();
                            averageDistanceDifference += Math.abs(total - averageDistanceDifference);
                        }
                    }
                    averageDistanceDifference /= entries.size();
                    averageDistancePercentage /= averageTicks;
                    averageTicks /= entries.size();
                    player.debug(true, false, false,
                            averageDistanceDifference,
                            averageDistancePercentage,
                            averageTicks
                    );
                }
            });
        }
        return false;
    }

    @Override
    public void scheduler() {
        this.schedulerMap.put(TPS.getTick(player), player.movement.getCustomDistance());
    }

    @Override
    protected boolean canDo() {
        return player.canRunCheck(hackType, true);
    }
}