package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.event.PlayerAttackEvent;
import com.vagdedes.spartan.listeners.protocol.async.LagCompensation;
import org.bukkit.entity.Player;

public class Attack extends PacketAdapter {

    public Attack() {
        super(
                Register.plugin,
                ListenerPriority.HIGHEST,
                PacketType.Play.Client.USE_ENTITY
        );
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        PacketContainer packet = event.getPacket();
        int id = packet.getIntegers().read(0);

        if (packet.getEnumEntityUseActions().read(0).getAction() == EnumWrappers.EntityUseAction.ATTACK) {
            if (LagCompensation.getPlayerByID(id) != null) {
                Shared.attack(
                        new PlayerAttackEvent(
                                player,
                                LagCompensation.getPlayerByID(id),
                                false
                        )
                );
            }
        }
    }
}
