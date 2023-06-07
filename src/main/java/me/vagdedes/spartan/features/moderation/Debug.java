package me.vagdedes.spartan.features.moderation;

import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.configuration.Messages;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.SpartanBukkit;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class Debug {

    private static final Map<UUID, Map<UUID, String>> hm = new LinkedHashMap<>(Config.getMaxPlayers());

    public static void clear() {
        hm.clear();
    }

    public static boolean has(SpartanPlayer p, SpartanPlayer t, String s) {
        Map<UUID, String> map = hm.get(p.getUniqueId());

        if (map != null) {
            String data = map.get(t.getUniqueId());
            return data != null && data.contains("[" + s + "]");
        }
        return false;
    }

    public static boolean has(SpartanPlayer p, SpartanPlayer t) {
        Map<UUID, String> map = hm.get(p.getUniqueId());

        if (map != null) {
            String data = map.get(t.getUniqueId());
            return data != null;
        }
        return false;
    }

    public static void remove(SpartanPlayer p, SpartanPlayer t) {
        final UUID uuid = p.getUniqueId();
        final UUID target = t.getUniqueId();
        final Map<UUID, String> map = hm.get(uuid);

        if (map != null && map.containsKey(target)) {
            hm.get(uuid).remove(target);

            if ((map.size() - 1) == 0) {
                hm.remove(uuid);
            }
            String msg = Messages.get("debug_disable_all_message");
            msg = msg.replace("{player}", t.getName());
            p.getPlayer().sendMessage(msg);
        }
    }

    public static void add(SpartanPlayer p, SpartanPlayer t, String s) {
        boolean enable = false;
        final UUID uuid = p.getUniqueId();
        final UUID target = t.getUniqueId();
        Map<UUID, String> map = hm.get(uuid);

        if (map == null) {
            map = new LinkedHashMap<>();
            hm.put(uuid, map);
        }
        String data = map.get(target);

        if (data != null) {
            if (data.contains("[" + s + "]")) {
                hm.get(uuid).put(target, data = data.replace("[" + s + "]", ""));

                if (data.length() == 0) {
                    hm.get(uuid).remove(target);

                    if ((map.keySet().size() - 1) == 0) {
                        hm.remove(uuid);
                    }
                }
            } else {
                hm.get(uuid).put(target, data + "[" + s + "]");
                enable = true;
            }
        } else {
            hm.get(uuid).put(target, "[" + s + "]");
            enable = true;
        }
        String msg = Messages.get(enable ? "debug_enable_message" : "debug_disable_message");
        msg = msg.replace("{player}", t.getName());
        msg = msg.replace("{type}", s);
        p.getPlayer().sendMessage(msg);
    }

    public static void runMove(SpartanPlayer p, String s) {
        inform(p, "Movement", s);
    }

    public static void runCombat(SpartanPlayer p, String s) {
        inform(p, "Combat", s);
    }

    public static void runMisc(SpartanPlayer p, String s) {
        inform(p, "Misc", s);
    }

    private static void inform(SpartanPlayer p, String t, String s) {
        for (Map.Entry<UUID, Map<UUID, String>> entry : hm.entrySet()) {
            String data = entry.getValue().get(p.getUniqueId());

            if (data != null && data.contains("[" + t + "]")) {
                SpartanPlayer owner = SpartanBukkit.getPlayer(entry.getKey());

                if (owner != null) {
                    String msg = Messages.get("debug_player_message");
                    msg = msg.replace("{player}", p.getName());
                    msg = msg.replace("{type}", t);
                    msg = msg.replace("{info}", s.toLowerCase().replace("_", "-"));
                    owner.sendMessage(msg);
                }
            }
        }
    }

    public static boolean canRun() {
        return !hm.isEmpty();
    }
}
