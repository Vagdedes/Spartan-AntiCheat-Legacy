package ai.idealistic.vacan.functionality.command;

import ai.idealistic.vacan.abstraction.check.CheckEnums;
import ai.idealistic.vacan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.vacan.functionality.server.Permissions;
import ai.idealistic.vacan.functionality.server.PluginBase;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class CommandTab implements TabCompleter {

    private static final Map<String, CheckEnums.Permission[]> commands = new LinkedHashMap<>(18); // Attention

    static {
        commands.put("menu", new CheckEnums.Permission[]{CheckEnums.Permission.INFO, CheckEnums.Permission.MANAGE});
        commands.put("panic", new CheckEnums.Permission[]{CheckEnums.Permission.MANAGE});
        commands.put("toggle", new CheckEnums.Permission[]{CheckEnums.Permission.MANAGE});
        commands.put("rl", new CheckEnums.Permission[]{CheckEnums.Permission.RELOAD});
        commands.put("reload", new CheckEnums.Permission[]{CheckEnums.Permission.RELOAD});
        commands.put("notifications", new CheckEnums.Permission[]{CheckEnums.Permission.NOTIFICATIONS});
        commands.put("verbose", new CheckEnums.Permission[]{CheckEnums.Permission.NOTIFICATIONS});
        commands.put("info", new CheckEnums.Permission[]{CheckEnums.Permission.INFO});
        commands.put("kick", new CheckEnums.Permission[]{CheckEnums.Permission.KICK});
        commands.put("warn", new CheckEnums.Permission[]{CheckEnums.Permission.WARN});
        commands.put("bypass", new CheckEnums.Permission[]{CheckEnums.Permission.USE_BYPASS});
        commands.put("conditions", new CheckEnums.Permission[]{CheckEnums.Permission.CONDITION});
        commands.put("moderation", new CheckEnums.Permission[]{
                CheckEnums.Permission.KICK,
                CheckEnums.Permission.WARN,
                CheckEnums.Permission.USE_BYPASS,
                CheckEnums.Permission.WAVE
        });
        commands.put("proxy-command", new CheckEnums.Permission[]{});
        commands.put("wave add", new CheckEnums.Permission[]{CheckEnums.Permission.WAVE});
        commands.put("wave remove", new CheckEnums.Permission[]{CheckEnums.Permission.WAVE});
        commands.put("wave clear", new CheckEnums.Permission[]{CheckEnums.Permission.WAVE});
        commands.put("wave run", new CheckEnums.Permission[]{CheckEnums.Permission.WAVE});
        commands.put("wave list", new CheckEnums.Permission[]{CheckEnums.Permission.WAVE});
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String arg, String[] args) {
        List<String> list = new ArrayList<>();
        int length = args.length;

        if (length == 1) {
            boolean isPlayer = sender instanceof Player;
            Player p = isPlayer ? (Player) sender : null;
            String argAbstract = args[0].toLowerCase();

            for (Map.Entry<String, CheckEnums.Permission[]> entry : commands.entrySet()) {
                boolean add;

                if (!isPlayer) {
                    add = true;
                } else {
                    add = false;

                    if (entry.getValue().length > 0) {
                        for (CheckEnums.Permission permission : entry.getValue()) {
                            if (Permissions.has(p, permission)) {
                                add = true;
                                break;
                            }
                        }
                    } else {
                        add = Permissions.has(p, CheckEnums.Permission.ADMIN);
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
            Collection<PlayerProtocol> protocols = PluginBase.getProtocols();

            if (!protocols.isEmpty()) {
                String argAbstract = args[length - 1].toLowerCase();
                boolean player = sender instanceof Player;
                PlayerProtocol p = player ? PluginBase.getProtocol((Player) sender) : null;
                player &= p != null;

                for (PlayerProtocol protocol : protocols) {
                    if (!player || p.bukkit().canSee(protocol.bukkit())) {
                        String name = protocol.bukkit().getName();

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
