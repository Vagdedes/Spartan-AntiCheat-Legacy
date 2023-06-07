package me.vagdedes.spartan.features.performance;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.handlers.stability.TestServer;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.SpartanBukkit;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class MaximumCheckedPlayers {

    private static final CopyOnWriteArrayList<UUID> list = new CopyOnWriteArrayList<>();
    private static final int minimumPlayers = 20, refreshTicks = 20;
    public static final String option = "Performance.maximum_checked_players_at_once";
    private static int ticks = 0;

    static {
        if (Register.isPluginLoaded()) {
            SpartanBukkit.runRepeatingTask(() -> {
                int optionAmount = Settings.getInteger(option);

                if (optionAmount > 0) {
                    if (ticks == 0) {
                        ticks = refreshTicks;
                        optionAmount = Math.max(optionAmount, minimumPlayers);
                        List<SpartanPlayer> players = SpartanBukkit.getPlayers();
                        int playerAmount = players.size();

                        if (playerAmount <= optionAmount) {
                            for (SpartanPlayer player : players) {
                                list.add(player.getUniqueId());
                            }
                        } else {
                            int toAddSize = 0;
                            Set<UUID> toAdd = new HashSet<>(playerAmount);

                            for (SpartanPlayer player : players) {
                                UUID uuid = player.getUniqueId();

                                if (!list.contains(uuid)) {
                                    toAdd.add(uuid);

                                    if ((toAddSize = toAdd.size()) == optionAmount) {
                                        list.clear();
                                        list.addAll(toAdd);
                                        return;
                                    }
                                }
                            }

                            if (toAddSize > 0) {
                                int newAmount = list.size() + toAddSize;

                                if (newAmount > optionAmount) {
                                    newAmount -= optionAmount;
                                    optionAmount = 0;
                                    Iterator<UUID> iterator = list.iterator();

                                    while (iterator.hasNext() && optionAmount < newAmount) {
                                        iterator.next();
                                        iterator.remove();
                                        optionAmount++;
                                    }
                                }
                                list.addAll(toAdd);
                            }
                        }
                    } else {
                        ticks -= 1;
                    }
                } else {
                    ticks = refreshTicks;
                }
            }, 1L, 1L);
        }
    }

    static boolean isActive() {
        int optionAmount = Settings.getInteger(option);
        return optionAmount > 0
                && SpartanBukkit.getPlayerCount() > Math.max(optionAmount, minimumPlayers);
    }

    public static void remove(SpartanPlayer player) {
        list.remove(player.getUniqueId());
    }

    private static boolean add(UUID uuid, int optionAmount) {
        return list.size() < Math.max(optionAmount, minimumPlayers)
                && list.addIfAbsent(uuid);
    }

    public static boolean add(SpartanPlayer player) {
        int optionAmount = Settings.getInteger(option);
        return optionAmount > 0 && add(player.getUniqueId(), optionAmount);
    }

    public static boolean isChecked(UUID uuid) {
        if (TestServer.isIdentified()) {
            return true;
        }
        int optionAmount = Settings.getInteger(option);
        return optionAmount > 0 && (list.contains(uuid) || add(uuid, optionAmount));
    }
}
