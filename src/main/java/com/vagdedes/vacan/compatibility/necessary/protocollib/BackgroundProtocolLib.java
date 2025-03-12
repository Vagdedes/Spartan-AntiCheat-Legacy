package com.vagdedes.vacan.compatibility.necessary.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.vagdedes.vacan.functionality.server.MultiVersion;
import com.vagdedes.vacan.listeners.protocol.*;
import com.vagdedes.vacan.listeners.protocol.combat.CombatListener;
import com.vagdedes.vacan.listeners.protocol.combat.LegacyCombatListener;
import com.vagdedes.vacan.listeners.protocol.standalone.BlockPlaceBalancerListener;
import com.vagdedes.vacan.listeners.protocol.standalone.EntityActionListener;
import com.vagdedes.vacan.listeners.protocol.standalone.JoinListener;
import com.vagdedes.vacan.utils.minecraft.entity.PlayerUtils;

import java.util.ArrayList;
import java.util.List;

public class BackgroundProtocolLib {

    private static final List<String> packets = new ArrayList<>();

    static boolean isPacketSupported(String packet) {
        return packets.contains(packet);
    }

    private static void handle() {
        for (PacketType type : PacketType.Play.Client.getInstance()) {
            packets.add(type.name());
        }
    }

    static void run() {
        handle();
        ProtocolManager p = ProtocolLibrary.getProtocolManager();
        p.addPacketListener(new JoinListener());
        p.addPacketListener(new EntityActionListener());
        p.addPacketListener(new VelocityListener());

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
            p.addPacketListener(new CombatListener());
        } else {
            p.addPacketListener(new LegacyCombatListener());
        }

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            p.addPacketListener(new MultiBlockListener());
        } else {
            p.addPacketListener(new MultiBlockLegacyListener());
        }

        p.addPacketListener(new MovementListener());
        p.addPacketListener(new TeleportListener());
        p.addPacketListener(new VehicleHandle());
        p.addPacketListener(new DeathListener());
        p.addPacketListener(new BlockPlaceBalancerListener());
        p.addPacketListener(new BlockPlaceListener());
        p.addPacketListener(new ClicksListener());
        p.addPacketListener(new PacketPistonHandle());
        p.addPacketListener(new ExplosionListener());
        p.addPacketListener(new PacketServerBlockHandle());
        p.addPacketListener(new PacketLatencyHandler());
        p.addPacketListener(new AbilitiesListener());
        p.addPacketListener(new UseItemStatusHandle());
        p.addPacketListener(new UseEntityListener());

        if (PlayerUtils.trident) {
            p.addPacketListener(new TridentListener());
        }
        if (false) {
            p.addPacketListener(new PacketDebug());
        }
    }

}
