package com.vagdedes.spartan.abstraction.data;

import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.TPS;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Trackers {

    public enum TrackerFamily {
        Velocity, Constant, Environment, Motion, Part
    }

    public enum TrackerType {
        SensitiveBlockBreak(TrackerFamily.Environment), TowerBuilding(TrackerFamily.Environment),
        BridgeBuilding(TrackerFamily.Environment),

        AbstractVelocity(TrackerFamily.Velocity), BouncingBlocks(TrackerFamily.Velocity),
        Piston(TrackerFamily.Velocity), BubbleWater(TrackerFamily.Velocity),
        ExtremeCollision(TrackerFamily.Velocity), Trident(TrackerFamily.Velocity),

        ElytraWear(TrackerFamily.Part), Vehicle(TrackerFamily.Part),
        GameMode(TrackerFamily.Part),

        ElytraUse(TrackerFamily.Motion);

        // Separator

        public final TrackerFamily family;

        TrackerType(TrackerFamily family) {
            this.family = family;
        }
    }

    private final Map<TrackerType, Long> enable, disable;
    private final Map<TrackerType, Map<String, Long>> child;
    private final SpartanPlayer player;

    public Trackers(SpartanPlayer player) {
        int length = TrackerType.values().length;
        this.player = player;
        this.enable = Collections.synchronizedMap(new LinkedHashMap<>(length));
        this.disable = Collections.synchronizedMap(new LinkedHashMap<>(length));
        this.child = Collections.synchronizedMap(new LinkedHashMap<>(length));
    }

    public void add(TrackerType handlerType) {
        if (!isDisabled(handlerType)) {
            synchronized (enable) {
                enable.put(handlerType, -1L);
            }
        }
    }

    public void add(TrackerType handlerType, int ticks) {
        if (!isDisabled(handlerType)) {
            synchronized (enable) {
                enable.put(
                        handlerType,
                        Math.max(
                                enable.getOrDefault(handlerType, 0L),
                                TPS.getTick(player) + ticks
                        )
                );
            }
        }
    }

    public void add(TrackerType handlerType, String key) {
        if (!isDisabled(handlerType)) {
            synchronized (child) {
                child.computeIfAbsent(handlerType, k -> new LinkedHashMap<>()).put(key, -1L);
            }
        }
    }

    public void add(TrackerType handlerType, String key, int ticks) {
        if (!isDisabled(handlerType)) {
            synchronized (child) {
                Map<String, Long> map = child.computeIfAbsent(handlerType, k -> new LinkedHashMap<>());
                map.put(
                        key,
                        Math.max(
                                map.getOrDefault(key, 0L),
                                TPS.getTick(player) + ticks
                        )
                );
            }
        }
    }

    public void disable(TrackerType handlerType, int ticks) {
        synchronized (disable) {
            disable.put(handlerType,
                    Math.max(
                            disable.getOrDefault(handlerType, 0L),
                            TPS.getTick(player) + ticks
                    )
            );
        }
    }

    public void removeMany(TrackerFamily handlerFamily) {
        for (TrackerType handlerType : TrackerType.values()) {
            if (handlerType.family == handlerFamily) {
                remove(handlerType);
            }
        }
    }

    public void remove(TrackerType handlerType) {
        synchronized (enable) {
            synchronized (child) {
                enable.remove(handlerType);
                child.remove(handlerType);
            }
        }
    }

    public void remove(TrackerType handlerType, String key) {
        synchronized (child) {
            Map<String, Long> map = child.get(handlerType);

            if (map != null
                    && map.remove(key) != null
                    && map.isEmpty()) {
                child.remove(handlerType);
            }
        }
    }

    private boolean has(long ticks) {
        return ticks == -1L || ticks > TPS.getTick(player);
    }

    public boolean has() {
        if (!enable.isEmpty()) {
            synchronized (enable) {
                for (Long ticks : enable.values()) {
                    if (has(ticks)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean has(TrackerFamily handlerFamily) {
        for (TrackerType handlerType : TrackerType.values()) {
            if (handlerType.family == handlerFamily && this.has(handlerType)) {
                return true;
            }
        }
        return false;
    }

    public boolean has(TrackerType handlerType) {
        Long ticks;

        synchronized (enable) {
            ticks = enable.get(handlerType);
        }
        return ticks != null && has(ticks);
    }

    public boolean has(TrackerType handlerType, String key) {
        synchronized (child) {
            Map<String, Long> map = child.get(handlerType);

            if (map != null) {
                Long ticks = map.get(key);
                return ticks != null && has(ticks);
            } else {
                return false;
            }
        }
    }

    public boolean isDisabled(TrackerType handlerType) {
        Long ticks;

        synchronized (disable) {
            ticks = disable.get(handlerType);
        }
        return ticks != null && ticks >= TPS.getTick(player);
    }

    public int getRemainingTicks(TrackerType handlerType, String key) {
        synchronized (child) {
            Map<String, Long> map = child.get(handlerType);
            return map == null ? 0 : getRemainingTicks(map.get(key));
        }
    }

    public int getRemainingTicks(TrackerType handlerType) {
        Long ticks;

        synchronized (enable) {
            ticks = enable.get(handlerType);
        }
        return getRemainingTicks(ticks);
    }

    private int getRemainingTicks(Long ticks) {
        if (ticks == null) {
            return 0;
        } else {
            ticks -= TPS.getTick(player);
            return ticks < 0 ? 0 : ticks.intValue();
        }
    }

}
