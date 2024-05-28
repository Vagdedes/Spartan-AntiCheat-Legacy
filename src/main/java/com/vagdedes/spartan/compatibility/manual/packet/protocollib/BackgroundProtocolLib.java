package com.vagdedes.spartan.compatibility.manual.packet.protocollib;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.vagdedes.spartan.listeners.protocol.EntityAction;
import com.vagdedes.spartan.listeners.protocol.Join;
import com.vagdedes.spartan.listeners.protocol.Move;
import com.vagdedes.spartan.listeners.protocol.Velocity;

public class BackgroundProtocolLib {

    static void run() {
        ProtocolManager p = ProtocolLibrary.getProtocolManager();
        p.addPacketListener(new Join());
        p.addPacketListener(new EntityAction());
        p.addPacketListener(new Velocity());
        Move.registerPacketListeners(p);
    }
}
