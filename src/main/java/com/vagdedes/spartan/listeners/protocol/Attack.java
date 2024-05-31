package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.vagdedes.spartan.Register;

import com.vagdedes.spartan.utils.math.RayUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.joml.Vector2f;
import org.joml.Vector3d;


import java.util.Optional;

import static java.lang.Math.PI;

public class Attack extends PacketAdapter {
    public Attack() {
        super(Register.plugin, ListenerPriority.MONITOR,
                        PacketType.Play.Client.USE_ENTITY);
    }
    @Override
    public void onPacketReceiving(PacketEvent event) {

        Bukkit.getScheduler().runTask(Register.plugin, () -> {
            Player player = event.getPlayer();
            Entity entity = ProtocolLibrary.getProtocolManager().
                            getEntityFromID(event.getPlayer().getWorld(), event.getPacket().getIntegers().read(0));
            PacketContainer packet = event.getPacket();
            //player.sendMessage(RayUtils.castTo360(player.getLocation().getYaw())
            //                + " " + getRotation(player, entity)[0]);

        });

    }
    public Vector2f calculate(final Vector3d from, final Vector3d to) {
        final Vector3d diff = to.sub(from);
        final double distance = Math.hypot(diff.x(), diff.z());
        final float yaw = (float) (Math.atan2(diff.z(), diff.x()) * 180.0F / PI) - 90.0F;
        final float pitch = (float) (-(Math.atan2(diff.y(), distance) * 180.0F / PI));
        return new Vector2f(yaw, pitch);
    }
    private static float[] getRotation(Player player1, Entity entity2) {
        Vector direction = entity2.getLocation().toVector().subtract(player1.getLocation().toVector());

        float yaw = (float) Math.toDegrees(Math.atan2(direction.getX(), direction.getZ()));

        float pitch = (float) Math.toDegrees(Math.asin(-direction.getY()));

        return new float[]{RayUtils.castTo360(yaw), pitch};
    }
}
