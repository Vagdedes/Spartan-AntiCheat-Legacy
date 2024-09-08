package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.event.PlayerAttackEvent;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.bukkit.Event_Combat;
import com.vagdedes.spartan.utils.java.OverflowMap;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Packet_Combat extends PacketAdapter {

    private final Map<UUID, Integer> pendingAttacks = new OverflowMap<>(new ConcurrentHashMap<>(), 1_024);

    public Packet_Combat() {
        super(
                Register.plugin,
                ListenerPriority.HIGHEST,
                PacketType.Play.Client.USE_ENTITY,
                PacketType.Play.Server.DAMAGE_EVENT
        );
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.USE_ENTITY) {
            SpartanProtocol protocol = SpartanBukkit.getProtocol(event.getPlayer());

            if (protocol.spartanPlayer.bedrockPlayer) {
                return;
            }
            PacketContainer packet = event.getPacket();
            int entityId = packet.getIntegers().read(0);

            if ((!packet.getEntityUseActions().getValues().isEmpty()) ?
                    packet.getEntityUseActions().read(0).equals(EnumWrappers.EntityUseAction.ATTACK)
                    : packet.getEnumEntityUseActions().read(0).getAction().equals(
                    EnumWrappers.EntityUseAction.ATTACK)) {
                Player player = event.getPlayer();
                pendingAttacks.put(player.getUniqueId(), entityId);
                useEntity(
                        new PlayerAttackEvent(
                                player,
                                SpartanBukkit.getProtocol(player).player,
                                false
                        )
                );
            }
        }
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.DAMAGE_EVENT) {
            SpartanProtocol protocol = SpartanBukkit.getProtocol(event.getPlayer());

            if (protocol.spartanPlayer.bedrockPlayer) {
                return;
            }
            PacketContainer packet = event.getPacket();
            int entityId = packet.getIntegers().read(0);

            pendingAttacks.entrySet().removeIf(entry -> {
                UUID playerUUID = entry.getKey();
                int pendingEntityId = entry.getValue();

                if (pendingEntityId == entityId) {
                    Player attacker = plugin.getServer().getPlayer(playerUUID);

                    if (attacker != null) {
                        Event_Combat.deal(
                                new PlayerAttackEvent(
                                        attacker,
                                        protocol.player,
                                        false
                                ),
                                true
                        );
                    }
                    return true;
                }
                return false;
            });
        }
    }

    private static void useEntity(PlayerAttackEvent e) {
        SpartanBukkit.getProtocol(e.getPlayer()).spartanPlayer.getExecutor(
                Enums.HackType.KillAura
        ).handle(e.isCancelled(), e);
    }

}
