package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.bukkit.Event_BlockPlace;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

public class Packet_BlockPlace extends PacketAdapter {

    public Packet_BlockPlace() {
        super(Register.plugin, ListenerPriority.HIGHEST,
                        PacketType.Play.Client.USE_ITEM,
                        PacketType.Play.Client.BLOCK_PLACE
        );
        // Method: Event_BlockPlace.event()
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        SpartanProtocol protocol = SpartanBukkit.getProtocol(player);
        PacketContainer packet = event.getPacket();
        if (isPlacingBlock(packet)) {
            if (packet.getBlockPositionModifier().getValues().isEmpty() && packet.getMovingBlockPositions().getValues().isEmpty()) {
                // stub for debug
            } else {

                BlockPosition blockPosition = new BlockPosition(0, 0, 0);
                EnumWrappers.Direction direction = null;
                if (!packet.getMovingBlockPositions().getValues().isEmpty()) {
                    blockPosition = packet.getMovingBlockPositions().read(0).getBlockPosition();
                    direction = packet.getMovingBlockPositions().read(0).getDirection();
                }
                if (!packet.getBlockPositionModifier().getValues().isEmpty()) {
                    blockPosition = packet.getBlockPositionModifier().read(0);
                    direction = packet.getDirections().read(0);
                }

                Location l = new Location(player.getWorld(), blockPosition.toVector().getBlockX(), blockPosition.toVector().getBlockY(),  blockPosition.toVector().getBlockZ());
                l.add(BlockFace.valueOf(direction.name()).getDirection());
                World world = player.getWorld();
                Block block = world.getBlockAt((int) l.getX(), (int) l.getY(), (int) l.getZ());
                BlockPlaceEvent blockPlaceEvent =
                                new BlockPlaceEvent(
                                block,
                                block.getState(), player.getLocation().getBlock(),
                                player.getInventory().getItemInMainHand(), player, true);
                Event_BlockPlace.event(blockPlaceEvent);
            }
        } else {
            protocol.rightClickCounter++;
        }
    }

    public boolean isPlacingBlock(PacketContainer packet) {
        BlockPosition blockPosition = new BlockPosition(0, 0, 0);
        if (packet.getHands().getValues().isEmpty()) {
            if (!packet.getMovingBlockPositions().getValues().isEmpty()) {
                blockPosition = packet.getMovingBlockPositions().read(0).getBlockPosition();
            }
            if (!packet.getBlockPositionModifier().getValues().isEmpty()) {
                blockPosition = packet.getBlockPositionModifier().read(0);
            }
            return (blockPosition.getY() != -1);
        } else {
            return (packet.getType().equals(PacketType.Play.Client.USE_ITEM)
                            && packet.getHands().read(0).equals(EnumWrappers.Hand.MAIN_HAND));
        }
    }

}