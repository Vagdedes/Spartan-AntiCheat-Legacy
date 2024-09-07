package com.vagdedes.spartan.listeners.protocol.bukkit.replicated.partly.completed;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.listeners.bukkit.protocol.replicated.Event_Vehicle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

public class Packet_Vehicle extends PacketAdapter {

    public Packet_Vehicle() {
        super(
                Register.plugin,
                ListenerPriority.HIGHEST,
                PacketType.Play.Client.STEER_VEHICLE,
                PacketType.Play.Server.ATTACH_ENTITY
        );
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.STEER_VEHICLE) {
            boolean dismount = event.getPacket().getBooleans().read(1);  // The dismount flag

            if (dismount) {
                VehicleExitEvent bukkitEvent = new VehicleExitEvent(
                        null, // todo
                        event.getPlayer()
                );
                bukkitEvent.setCancelled(event.isCancelled());
                Event_Vehicle.exit(bukkitEvent);
            }
        }
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.ATTACH_ENTITY) {
            Entity entity = event.getPacket().getEntityModifier(event).read(1);

            if (entity instanceof Player) {
                VehicleEnterEvent bukkitEvent = new VehicleEnterEvent(
                        null, // todo
                        event.getPlayer()
                );
                bukkitEvent.setCancelled(event.isCancelled());
                Event_Vehicle.enter(bukkitEvent);
            }
        }
    }

}