package com.vagdedes.spartan.listeners.protocol.bukkit.replicated.partly.completed;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.bukkit.protocol.replicated.Event_World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;

public class Packet_World extends PacketAdapter {

    public Packet_World() {
        super(
                Register.plugin,
                ListenerPriority.HIGHEST,
                PacketType.Play.Client.BLOCK_DIG,
                PacketType.Play.Client.BLOCK_PLACE,
                PacketType.Play.Client.ARM_ANIMATION
        );
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        SpartanPlayer spartanPlayer = SpartanBukkit.getProtocol(player).spartanPlayer;
        PacketType packetType = event.getPacketType();

        if (packetType.equals(PacketType.Play.Client.BLOCK_DIG)) {
            Block block = event.getPacket().getBlockPositionModifier().read(0).toLocation(
                    player.getWorld()
            ).getBlock();
            BlockBreakEvent bukkitEvent = new BlockBreakEvent(
                    block,
                    player
            );
            bukkitEvent.setCancelled(event.isCancelled());
            Event_World.blockBreak(bukkitEvent);
        } else if (packetType.equals(PacketType.Play.Client.BLOCK_PLACE)) {
            boolean cancelled = event.isCancelled();

            // invalid code
            /*
            Block block = event.getPacket().getBlockPositionModifier().read(0).toLocation(
                    player.getWorld()
            ).getBlock();
            BlockPlaceEvent bukkitEvent = new BlockPlaceEvent(
                    block,
                    null, // todo
                    null, // todo
                    null, // todo
                    player,
                    !cancelled,
                    null // todo
            );
            bukkitEvent.setCancelled(event.isCancelled());
            Event_World.blockPlace(bukkitEvent);

             */
        } else if (packetType.equals(PacketType.Play.Client.ARM_ANIMATION)) {
            PlayerAnimationEvent bukkitEvent = new PlayerAnimationEvent(
                    player,
                    PlayerAnimationType.ARM_SWING
            );
            bukkitEvent.setCancelled(event.isCancelled());
            Event_World.animation(bukkitEvent);
        }
    }

}
