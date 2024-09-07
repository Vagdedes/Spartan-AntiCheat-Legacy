package com.vagdedes.spartan.listeners.protocol.bukkit.replicated.not.implemented;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.world.SpartanBlock;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.bukkit.standalone.chunks.Event_Chunks;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

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
            handleBlockBreak(event, spartanPlayer);
        } else if (packetType.equals(PacketType.Play.Client.BLOCK_PLACE)) {
            handleBlockPlace(event, spartanPlayer);
        } else if (packetType.equals(PacketType.Play.Client.ARM_ANIMATION)) {
            handlePlayerAnimation(event, spartanPlayer);
        }
    }

    private void handleBlockBreak(PacketEvent event, SpartanPlayer p) {
        Block block = event.getPacket().getBlockPositionModifier().read(0).toLocation(p.getWorld()).getBlock();
        Event_Chunks.cache(block.getChunk(), false);
        SpartanBlock spartanBlock = new SpartanBlock(block);
        p.movement.judgeGround();
        p.getExecutor(Enums.HackType.NoSwing).handle(event.isCancelled(), event);
        p.getExecutor(Enums.HackType.BlockReach).handle(event.isCancelled(), event);
        p.getExecutor(Enums.HackType.FastBreak).handle(event.isCancelled(), event);
        p.getExecutor(Enums.HackType.GhostHand).handle(event.isCancelled(), spartanBlock);
    }

    private void handleBlockPlace(PacketEvent event, SpartanPlayer p) {
        Block block = event.getPacket().getBlockPositionModifier().read(0).toLocation(p.getWorld()).getBlock();
        Event_Chunks.cache(block.getChunk(), false);
        p.movement.judgeGround();
        p.getExecutor(Enums.HackType.ImpossibleActions).handle(event.isCancelled(), event);
        p.getExecutor(Enums.HackType.BlockReach).handle(event.isCancelled(), event);
        p.getExecutor(Enums.HackType.FastPlace).handle(event.isCancelled(), event);
    }

    private void handlePlayerAnimation(PacketEvent event, SpartanPlayer p) {
        p.getExecutor(Enums.HackType.NoSwing).handle(event.isCancelled(), event);
    }
}
