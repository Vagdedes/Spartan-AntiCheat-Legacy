package me.vagdedes.spartan.handlers.commands;

import me.vagdedes.spartan.features.important.Permissions;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CommandTab implements TabCompleter {

    private static final Map<String, Enums.Permission[]> commands = new LinkedHashMap<>(18); // Attention

    static {
        commands.put("menu", new Enums.Permission[]{Enums.Permission.info, Enums.Permission.manage});
        commands.put("toggle", new Enums.Permission[]{Enums.Permission.manage});
        commands.put("rl", new Enums.Permission[]{Enums.Permission.reload});
        commands.put("reload", new Enums.Permission[]{Enums.Permission.reload});
        commands.put("notifications", new Enums.Permission[]{Enums.Permission.notifications});
        commands.put("info", new Enums.Permission[]{Enums.Permission.info});
        commands.put("kick", new Enums.Permission[]{Enums.Permission.kick});
        commands.put("warn", new Enums.Permission[]{Enums.Permission.warn});
        commands.put("bypass", new Enums.Permission[]{Enums.Permission.use_bypass});
        commands.put("ban", new Enums.Permission[]{Enums.Permission.ban});
        commands.put("unban", new Enums.Permission[]{Enums.Permission.unban});
        commands.put("ban-info", new Enums.Permission[]{Enums.Permission.ban, Enums.Permission.unban});
        commands.put("wave add", new Enums.Permission[]{Enums.Permission.wave});
        commands.put("wave remove", new Enums.Permission[]{Enums.Permission.wave});
        commands.put("wave clear", new Enums.Permission[]{Enums.Permission.wave});
        commands.put("wave run", new Enums.Permission[]{Enums.Permission.wave});
        commands.put("wave list", new Enums.Permission[]{Enums.Permission.wave});
        commands.put("report", new Enums.Permission[]{Enums.Permission.report});
        commands.put("customer-support", new Enums.Permission[]{Enums.Permission.admin});
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String arg, String[] args) {
        List<String> list = new LinkedList<>();
        int length = args.length;

        if (length == 1) {
            boolean isPlayer = sender instanceof Player;
            Player p = isPlayer ? (Player) sender : null;
            String argAbstract = args[0].toLowerCase();

            for (Map.Entry<String, Enums.Permission[]> entry : commands.entrySet()) {
                boolean add;

                if (!isPlayer) {
                    add = true;
                } else {
                    add = false;

                    for (Enums.Permission permission : entry.getValue()) {
                        if (Permissions.has(p, permission)) {
                            add = true;
                            break;
                        }
                    }
                }

                if (add) {
                    String key = entry.getKey();

                    if (key.contains(argAbstract)) {
                        list.add(key);
                    }
                }
            }
        } else if (length > 1) {
            List<SpartanPlayer> players = SpartanBukkit.getPlayers();

            if (players.size() > 0) {
                String argAbstract = args[length - 1].toLowerCase();
                boolean player = sender instanceof Player;
                SpartanPlayer p = player ? SpartanBukkit.getPlayer((Player) sender) : null;
                player &= p != null;

                for (SpartanPlayer o : players) {
                    Player on = o.getPlayer();

                    if (on != null && (!player || p.canSee(on))) {
                        String name = o.getName();

                        if (name.toLowerCase().contains(argAbstract)) {
                            list.add(name);
                        }
                    }
                }
            }
        }
        return list;
    }
}
