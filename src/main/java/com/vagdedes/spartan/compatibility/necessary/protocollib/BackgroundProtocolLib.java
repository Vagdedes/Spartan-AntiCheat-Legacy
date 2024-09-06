package com.vagdedes.spartan.compatibility.necessary.protocollib;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.vagdedes.spartan.listeners.protocol.Attack;
import com.vagdedes.spartan.listeners.protocol.bukkit.replicated.Packet_Velocity;
import com.vagdedes.spartan.listeners.protocol.bukkit.replicated.partly.completed.Packet_Movement;
import com.vagdedes.spartan.listeners.protocol.bukkit.replicated.partly.completed.Packet_Status;
import com.vagdedes.spartan.listeners.protocol.bukkit.replicated.partly.completed.Packet_Teleport;
import com.vagdedes.spartan.listeners.protocol.standalone.Packet_EntityAction;

public class BackgroundProtocolLib {

    static void run() {
        ProtocolManager p = ProtocolLibrary.getProtocolManager();
        p.addPacketListener(new Packet_Status());
        p.addPacketListener(new Packet_EntityAction());
        p.addPacketListener(new Packet_Velocity());
        p.addPacketListener(new Attack());
        p.addPacketListener(new Packet_Movement());
        p.addPacketListener(new Packet_Teleport());
    }

}
