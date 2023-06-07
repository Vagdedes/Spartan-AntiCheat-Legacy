package me.vagdedes.spartan.features.moderation;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.configuration.Messages;
import me.vagdedes.spartan.features.important.Permissions;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import me.vagdedes.spartan.utils.server.ConfigUtils;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Spectate {

    private static final Map<SpartanPlayer, SpartanPlayer> target = new LinkedHashMap<>(Config.getMaxPlayers());
    private static final Map<SpartanPlayer, SpartanLocation> initialLocation = new LinkedHashMap<>(Config.getMaxPlayers());

    static {
        if (Register.isPluginLoaded()) {
            SpartanBukkit.runRepeatingTask(() -> {
                Set<Map.Entry<SpartanPlayer, SpartanPlayer>> entries = target.entrySet();
                int size = entries.size();

                if (size > 0) {
                    Map<SpartanPlayer, Boolean> toRemove = new LinkedHashMap<>(size);

                    for (Map.Entry<SpartanPlayer, SpartanPlayer> entry : entries) {
                        SpartanPlayer p = entry.getKey();

                        SpartanBukkit.runTask(p, () -> {
                            if (!Permissions.has(p, Enums.Permission.info)
                                    && !Permissions.has(p, Enums.Permission.manage)) {
                                toRemove.put(p, true);
                            } else {
                                SpartanPlayer t = entry.getValue();

                                if (t == null) {
                                    toRemove.put(p, true);
                                } else {
                                    SpartanLocation location = p.getLocation();
                                    SpartanLocation targetLocation = t.getLocation();

                                    if (!location.getWorld().equals(targetLocation.getWorld())) {
                                        toRemove.put(p, false);
                                    } else if (AlgebraUtils.getHorizontalDistance(location, targetLocation) >= (MoveUtils.chunk * 3)) {
                                        toRemove.put(p, true);
                                    } else {
                                        appearance(p, false);
                                    }
                                }
                            }
                        });
                    }
                    for (Map.Entry<SpartanPlayer, Boolean> entry : toRemove.entrySet()) {
                        remove(entry.getKey(), entry.getValue());
                    }
                }
            }, 1L, 4L);
        }
    }

    public static void clear() {
        for (SpartanPlayer p : target.keySet()) {
            appearance(p, true);
        }
        target.clear();
        initialLocation.clear();
    }

    public static boolean isTarget(SpartanPlayer p, SpartanPlayer t) {
        SpartanPlayer target = getTarget(p);
        return target != null && target.equals(t);
    }

    public static boolean isUsing(SpartanPlayer p) {
        return getTarget(p) != null;
    }

    public static SpartanPlayer getTarget(SpartanPlayer p) {
        return target.get(p);
    }

    public static void remove(SpartanPlayer p, boolean teleport) {
        SpartanLocation location = initialLocation.get(p);

        if (location != null) {
            if (teleport) {
                p.teleport(location);
            }
            initialLocation.remove(p);
            target.remove(p);
            appearance(p, true);

            String message = ConfigUtils.replaceWithSyntax(p, Messages.get("spectating_ended"), null);
            p.sendMessage(message);
        }
    }

    public static void run(SpartanPlayer p, SpartanPlayer t) {
        if (!p.equals(t)) {
            SpartanLocation location = initialLocation.get(p);

            if (location != null) {
                initialLocation.put(p, location);
            } else {
                initialLocation.put(p, p.getLocation());
            }

            target.put(p, t);
            p.teleport(t.getLocation());
            appearance(p, false);

            String message = ConfigUtils.replaceWithSyntax(t, Messages.get("spectating_player"), null);
            p.sendInventoryCloseMessage(message); // Close inventory
        } else {
            p.sendInventoryCloseMessage(null);
        }
    }

    private static void appearance(SpartanPlayer p, boolean show) {
        if (show) {
            p.removePotionEffect(PotionEffectType.INVISIBILITY);
        } else {
            p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, true, false));
        }
        List<SpartanPlayer> players = SpartanBukkit.getPlayers();

        if (players.size() > 0) {
            for (SpartanPlayer so : players) {
                if (!so.equals(p)) {
                    if (show) {
                        if (!so.canSee(p)) {
                            so.showPlayer(p);
                        }
                    } else if (so.canSee(p)) {
                        so.hidePlayer(p);
                    }
                }
            }
        }
    }
}
