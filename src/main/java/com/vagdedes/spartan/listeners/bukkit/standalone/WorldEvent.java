package com.vagdedes.spartan.listeners.bukkit.standalone;

import com.vagdedes.spartan.abstraction.profiling.MiningHistory;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.functionality.server.PluginBase;
import com.vagdedes.spartan.functionality.tracking.Piston;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class WorldEvent implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void BlockBreak(BlockBreakEvent e) {
        PlayerProtocol protocol = PluginBase.getProtocol(e.getPlayer(), true);
        Block nb = e.getBlock();
        boolean cancelled = e.isCancelled();
        protocol.bukkitExtra.movement.judgeGround();

        // Detections
        if (!ItemsAdder.is(nb)) {
            protocol.profile().getRunner(Enums.HackType.NoSwing).handle(cancelled, e);
            protocol.profile().getRunner(Enums.HackType.BlockReach).handle(cancelled, e);
            protocol.profile().getRunner(Enums.HackType.FastBreak).handle(cancelled, e);
            protocol.profile().getRunner(Enums.HackType.GhostHand).handle(cancelled, nb);
        }
        protocol.profile().getRunner(Enums.HackType.Exploits).handle(cancelled, e);
        protocol.profile().getRunner(Enums.HackType.FastClicks).handle(cancelled, null);
        MiningHistory.log(protocol, nb, cancelled);

        if (protocol.profile().getRunner(Enums.HackType.NoSwing).prevent()
                || protocol.profile().getRunner(Enums.HackType.BlockReach).prevent()
                || protocol.profile().getRunner(Enums.HackType.FastBreak).prevent()
                || protocol.profile().getRunner(Enums.HackType.GhostHand).prevent()
                || protocol.profile().getRunner(Enums.HackType.XRay).prevent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Sign(SignChangeEvent e) {
        PlayerProtocol p = PluginBase.getProtocol(e.getPlayer(), true);

        // Detections
        p.profile().getRunner(Enums.HackType.Exploits).handle(e.isCancelled(), e.getLines());

        if (p.profile().getRunner(Enums.HackType.Exploits).prevent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Animation(PlayerAnimationEvent e) {
        PlayerProtocol protocol = PluginBase.getProtocol(e.getPlayer(), true);

        // Detections
        protocol.profile().getRunner(Enums.HackType.NoSwing).handle(e.isCancelled(), e);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Interact(PlayerInteractEvent e) {
        PlayerProtocol protocol = PluginBase.getProtocol(e.getPlayer(), true);
        Block nb = e.getClickedBlock();
        boolean notNull = nb != null,
                customBlock = notNull && ItemsAdder.is(nb);

        if (notNull) {
            // Detections
            if (!customBlock) {
                protocol.profile().getRunner(Enums.HackType.BlockReach).handle(false, e);
                protocol.profile().getRunner(Enums.HackType.FastBreak).handle(false, e);
                protocol.profile().getRunner(Enums.HackType.ImpossibleActions).handle(false, e);
            }
            protocol.profile().getRunner(Enums.HackType.FastEat).handle(false, e);

            if (!customBlock) {
                protocol.profile().getRunner(Enums.HackType.GhostHand).handle(false, e);
            }
        } else {
            // Detections
            protocol.profile().getRunner(Enums.HackType.FastEat).handle(false, e);
        }
        // Detections
        if (!customBlock) {
            protocol.profile().getRunner(Enums.HackType.NoSwing).handle(false, e);
        }
        protocol.profile().getRunner(Enums.HackType.FastBow).handle(false, e);

        if (protocol.profile().getRunner(Enums.HackType.GhostHand).prevent()
                || protocol.profile().getRunner(Enums.HackType.FastClicks).prevent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Piston(BlockPistonExtendEvent e) {
        if (!e.isCancelled()) {
            // Handlers
            Piston.run(e.getBlock(), e.getBlocks());
        }
    }

}