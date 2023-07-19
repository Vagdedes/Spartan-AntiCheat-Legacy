package me.vagdedes.spartan.system;

import me.vagdedes.spartan.checks.combat.VelocityCheck;
import me.vagdedes.spartan.checks.combat.criticals.Criticals;
import me.vagdedes.spartan.checks.combat.killAura.KillAura;
import me.vagdedes.spartan.checks.exploits.ChunkUpdates;
import me.vagdedes.spartan.checks.movement.MorePackets;
import me.vagdedes.spartan.checks.movement.irregularmovements.IrregularMovements;
import me.vagdedes.spartan.checks.movement.speed.Speed;
import me.vagdedes.spartan.checks.world.ImpossibleActions;
import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.features.moderation.Debug;
import me.vagdedes.spartan.features.moderation.Spectate;
import me.vagdedes.spartan.features.notifications.AwarenessNotifications;
import me.vagdedes.spartan.features.notifications.DetectionNotifications;
import me.vagdedes.spartan.features.protections.LagLeniencies;
import me.vagdedes.spartan.features.protections.PlayerLimitPerIP;
import me.vagdedes.spartan.features.protections.ReconnectCooldown;
import me.vagdedes.spartan.features.synchronicity.AutoUpdater;
import me.vagdedes.spartan.features.synchronicity.CrossServerInformation;
import me.vagdedes.spartan.features.synchronicity.cloud.CloudFeature;
import me.vagdedes.spartan.handlers.stability.DetectionLocation;
import me.vagdedes.spartan.handlers.stability.TPS;
import me.vagdedes.spartan.handlers.tracking.CombatProcessing;
import me.vagdedes.spartan.handlers.tracking.MovementProcessing;
import me.vagdedes.spartan.listeners.EventsHandler7;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.data.CooldownUtils;
import me.vagdedes.spartan.utils.gameplay.PlayerData;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Cache {

    // Management
    private static final List<Object> memory = new CopyOnWriteArrayList<>();

    // Calculations
    public static final int gradeDivisor = 1024;
    public static final boolean enoughRAM = (Runtime.getRuntime().maxMemory() / gradeDivisor / gradeDivisor) >= 900;

    public static Set<String> getCheckKeys(Set<String> set, String id) {
        Set<String> newSet = new HashSet<>(set.size());
        boolean bigID = id.length() == 2;
        char id0 = id.charAt(0);
        char id1 = bigID ? id.charAt(1) : '0';

        for (String name : set) {
            int length = name.length();

            if (length >= 3) {
                if (name.charAt(1) == '=') {
                    if (!bigID && name.charAt(0) == id0) {
                        newSet.add(name);
                    }
                } else if (name.charAt(2) == '=') {
                    if (bigID && name.charAt(0) == id0 && name.charAt(1) == id1) {
                        newSet.add(name);
                    }
                }
            } else if (length == 2) {
                if (bigID) {
                    if (name.charAt(0) == id0 && name.charAt(1) == id1) {
                        newSet.add(name);
                    }
                } else if (name.charAt(1) == '=' && name.charAt(0) == id0) {
                    newSet.add(name);
                }
            } else if (!bigID && length == 1 && name.charAt(0) == id0) {
                newSet.add(name);
            }
        }
        return newSet;
    }

    public static void clearCheckCache(SpartanPlayer p) {
        clearCheckCache(p, Enums.hackTypes());
    }

    public static void clearCheckCache(SpartanPlayer p, Enums.HackType[] hackTypes) {
        for (Enums.HackType hackType : hackTypes) {
            p.getBuffer(hackType).clear();
            p.getTimer(hackType).clear();
            p.getDecimals(hackType).clear();
            p.getCooldowns(hackType).clear();
            p.getTracker(hackType).clear();
        }
    }

    public static void clearCheckCache(SpartanPlayer p, Collection<Enums.HackType> hackTypes) {
        for (Enums.HackType hackType : hackTypes) {
            p.getBuffer(hackType).clear();
            p.getTimer(hackType).clear();
            p.getDecimals(hackType).clear();
            p.getCooldowns(hackType).clear();
            p.getTracker(hackType).clear();
        }
    }

    public static void clearCheckCache(SpartanPlayer p, Enums.CheckType checkType) {
        for (Enums.HackType hackType : Enums.hackTypes()) {
            if (hackType.getCheck().getCheckType() == checkType) {
                p.getBuffer(hackType).clear();
                p.getTimer(hackType).clear();
                p.getDecimals(hackType).clear();
                p.getCooldowns(hackType).clear();
                p.getTracker(hackType).clear();
            }
        }
    }

    public static void clearCheckCache(SpartanPlayer p, Enums.CheckType[] checkTypes) {
        for (Enums.HackType hackType : Enums.hackTypes()) {
            for (Enums.CheckType checkType : checkTypes) {
                if (hackType.getCheck().getCheckType() == checkType) {
                    p.getBuffer(hackType).clear();
                    p.getTimer(hackType).clear();
                    p.getDecimals(hackType).clear();
                    p.getCooldowns(hackType).clear();
                    p.getTracker(hackType).clear();
                    break;
                }
            }
        }
    }

    public static void clearStorage(boolean clear) {
        if (memory.size() > 0) {
            for (Object object : memory) {
                if (object instanceof Map) {
                    ((Map) object).clear();
                } else if (object instanceof Collection) {
                    ((Collection) object).clear();
                }
            }
            if (clear) {
                memory.clear();
            }
        }
    }

    public static void clear(SpartanPlayer p, Player n,
                             boolean leave, boolean distance, boolean deep,
                             SpartanLocation newLocation) {
        // Utils
        if (!leave) {
            PlayerData.run(p, n, n.getActivePotionEffects());
        }

        // Handlers
        if (!leave) {
            EventsHandler7.clear(p);
        }
        if (distance) {
            if (newLocation != null) {
                DetectionLocation.update(p, newLocation, false);
            } else {
                DetectionLocation.remove(p);
            }
        } else {
            DetectionLocation.remove(p);
        }
        p.resetHandlers();
        MovementProcessing.resetVehicleTicks(p);
        CombatProcessing.remove(p);

        // Objects
        if (distance) {
            p.getProfile().getCombat().endFights();
        }
        if (deep) {
            clearCheckCache(p);
        }

        // Features
        if (leave) {
            Spectate.remove(p, true);
        }

        // Detections
        if (newLocation != null) {
            IrregularMovements.refresh(p, newLocation);
            MorePackets.refresh(p, newLocation);
            ChunkUpdates.refresh(p, newLocation);
        } else {
            IrregularMovements.remove(p);
            MorePackets.remove(p);
            ChunkUpdates.remove(p);
        }
        if (leave || distance) {
            KillAura.remove(p);
        }
        Criticals.remove(p, leave);
        Speed.remove(p);
        ImpossibleActions.remove(p);
        VelocityCheck.refreshEntity(p);
    }

    public static <K, V> Map<K, V> store(Map<K, V> map) {
        memory.add(map);
        return map;
    }

    public static <T> List<T> store(List<T> list) {
        memory.add(list);
        return list;
    }

    public static <T> Set<T> store(Set<T> set) {
        memory.add(set);
        return set;
    }

    public static void disable() {
        if (CloudFeature.hasToken()) {
            AutoUpdater.complete();
        }

        // System
        TPS.clear();
        Threads.disable();
        SpartanBukkit.clear();

        // Utilities
        CooldownUtils.clear();

        // Features
        DetectionNotifications.clear();
        CrossServerInformation.clear();
        PlayerLimitPerIP.clear();
        Spectate.clear();
        ReconnectCooldown.clear();
        LagLeniencies.clear();
        AwarenessNotifications.clear();
        Debug.clear();

        // Configuration
        Config.create();
    }
}
