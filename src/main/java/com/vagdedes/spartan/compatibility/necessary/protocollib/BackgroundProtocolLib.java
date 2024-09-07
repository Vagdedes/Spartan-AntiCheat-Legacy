package com.vagdedes.spartan.compatibility.necessary.protocollib;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.vagdedes.spartan.listeners.protocol.bukkit.replicated.Packet_Movement;
import com.vagdedes.spartan.listeners.protocol.bukkit.replicated.Packet_Velocity;
import com.vagdedes.spartan.listeners.protocol.bukkit.replicated.partly.completed.*;
import com.vagdedes.spartan.listeners.protocol.standalone.Packet_EntityAction;
import com.vagdedes.spartan.listeners.protocol.standalone.Packet_Join;

public class BackgroundProtocolLib {

    static void run() {
        ProtocolManager p = ProtocolLibrary.getProtocolManager();
        p.addPacketListener(new Packet_Join());
        p.addPacketListener(new Packet_EntityAction());
        p.addPacketListener(new Packet_Velocity());
        p.addPacketListener(new Packet_Combat());
        p.addPacketListener(new Packet_Movement());
        p.addPacketListener(new Packet_Teleport());
        p.addPacketListener(new Packet_Health());
        p.addPacketListener(new Packet_Potion());
        p.addPacketListener(new Packet_Trident());
        p.addPacketListener(new Packet_Vehicle());
        p.addPacketListener(new Packet_World());
    }

}
