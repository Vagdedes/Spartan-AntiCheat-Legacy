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
import com.vagdedes.spartan.listeners.CustomHandlers;
import com.vagdedes.spartan.utils.java.OverflowMap;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Attack extends PacketAdapter {

    private final Map<UUID, Integer> pendingAttacks = new OverflowMap<>(new ConcurrentHashMap<>(), 1_024);

    public Attack() {
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
            handleUseEntity(event);
        }
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.DAMAGE_EVENT) {
            SpartanProtocol protocol = SpartanBukkit.getProtocol(event.getPlayer());

            if (protocol.spartanPlayer.bedrockPlayer) {
                return;
            }
            handleEntityDamage(event);
        }
    }

    private void handleUseEntity(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        int entityId = packet.getIntegers().read(0);

        if ((!packet.getEntityUseActions().getValues().isEmpty()) ?
                packet.getEntityUseActions().read(0).equals(EnumWrappers.EntityUseAction.ATTACK)
                : packet.getEnumEntityUseActions().read(0).getAction().equals(
                EnumWrappers.EntityUseAction.ATTACK)) {
            Player player = event.getPlayer();
            pendingAttacks.put(player.getUniqueId(), entityId);
            CustomHandlers.useEntity(
                    new PlayerAttackEvent(
                            player,
                            SpartanBukkit.getProtocol(player).player,
                            false
                    )
            );
        }
    }

    private void handleEntityDamage(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        int entityId = packet.getIntegers().read(0);

        pendingAttacks.entrySet().removeIf(entry -> {
            UUID playerUUID = entry.getKey();
            int pendingEntityId = entry.getValue();

            if (pendingEntityId == entityId) {
                Player attacker = plugin.getServer().getPlayer(playerUUID);

                if (attacker != null) {
                    SpartanProtocol protocol = SpartanBukkit.getProtocol(entityId);

                    if (protocol != null) {
                        CustomHandlers.attack(
                                new PlayerAttackEvent(
                                        attacker,
                                        protocol.player,
                                        false
                                )
                        );
                    }
                }
                return true;
            }
            return false;
        });
    }

}
