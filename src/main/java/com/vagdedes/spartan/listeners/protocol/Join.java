package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.player.protocol.SpartanProtocol;
import org.bukkit.Location;
import org.bukkit.event.Listener;

public class Join extends PacketAdapter implements Listener {

    public Join() {
        super(Register.plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.LOGIN);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        PacketContainer packet = event.getPacket();

        if (packet.getType() == PacketType.Play.Server.LOGIN) {
            SpartanProtocol protocol = new SpartanProtocol(event.getPlayer());
            protocol.spawnStatus = true;
            protocol.position = new Location(event.getPlayer().getWorld(), 0, 0, 0);
        }
    }

}
