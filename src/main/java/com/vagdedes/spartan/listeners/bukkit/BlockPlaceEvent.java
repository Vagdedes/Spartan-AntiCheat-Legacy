package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.functionality.server.PluginBase;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class BlockPlaceEvent implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public static void event(org.bukkit.event.block.BlockPlaceEvent e) {
        PlayerProtocol protocol = PluginBase.getProtocol(e.getPlayer(), true);

        if (event(protocol, e.getBlock(), e.getBlockAgainst(), e)) {
            e.setCancelled(true);
        }
    }

    public static boolean event(
            PlayerProtocol protocol,
            Block block,
            Block blockAgainst,
            Object event
    ) {
        protocol.bukkitExtra.movement.judgeGround();

        if (protocol.getWorld() != block.getWorld()) {
            return false;
        }
        protocol.profile().executeRunners(null, event);

        // Detections
        if (!ItemsAdder.is(block)) {
            protocol.profile().getRunner(Enums.HackType.ImpossibleActions).handle(
                    event,
                    new Block[]{block, blockAgainst}
            );
            protocol.profile().getRunner(Enums.HackType.BlockReach).handle(
                    event,
                    new Block[]{block, blockAgainst}
            );
            protocol.profile().getRunner(Enums.HackType.FastPlace).handle(event, block);
        }
        return protocol.profile().getRunner(Enums.HackType.FastPlace).prevent()
                || protocol.profile().getRunner(Enums.HackType.BlockReach).prevent()
                || protocol.profile().getRunner(Enums.HackType.ImpossibleActions).prevent();
    }

}
