package com.vagdedes.spartan.compatibility.manual.packet;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.listeners.protocol.EntityAction;
import com.vagdedes.spartan.listeners.protocol.Join;
import com.vagdedes.spartan.listeners.protocol.Move;
import com.vagdedes.spartan.listeners.protocol.Velocity;
import com.vagdedes.spartan.utils.java.ReflectionUtils;
import org.bukkit.entity.Player;

public class ProtocolLib {

    private static Class<?> temporaryPlayer = null;

    public static boolean isTemporaryPLayer(Player p) {
        return Compatibility.CompatibilityType.PROTOCOL_LIB.isFunctional()
                && temporaryPlayer != null
                && p.getClass().isInstance(temporaryPlayer);
    }

    public static void reload() {
        temporaryPlayer = ReflectionUtils.getClass("com.comphenix.protocol.injector.server.TemporaryPlayer");
        ProtocolManager p = ProtocolLibrary.getProtocolManager();
        p.addPacketListener(new Join());
        p.addPacketListener(new EntityAction());
        p.addPacketListener(new Velocity());
        Move.registerPacketListeners(p);
    }
}
