package com.vagdedes.spartan.listeners.protocol.bukkit.replicated.not.implemented;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class Packet_Health extends PacketAdapter {

    public Packet_Health() {
        super(Register.plugin, ListenerPriority.HIGHEST,
                        PacketType.Play.Server.ENTITY_STATUS,
                        PacketType.Play.Server.UPDATE_HEALTH);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        Entity entity = event.getPacket().getEntityModifier(event).read(0);

        if (entity instanceof Player) {
            Player player = (Player) entity;
            SpartanPlayer spartanPlayer = SpartanBukkit.getProtocol(player).spartanPlayer;

            if (event.getPacketType() == PacketType.Play.Server.ENTITY_STATUS) {
                handleFoodLevelChange(event, spartanPlayer);
            } else if (event.getPacketType() == PacketType.Play.Server.UPDATE_HEALTH) {
                handleHealthRegain(event, spartanPlayer);
            }
        }
    }

    private void handleFoodLevelChange(PacketEvent event, SpartanPlayer p) {
        p.getExecutor(Enums.HackType.FastEat).handle(event.isCancelled(), event);
        p.getExecutor(Enums.HackType.FastHeal).handle(event.isCancelled(), event);

        if (p.getExecutor(Enums.HackType.FastEat).prevent()) {
            event.setCancelled(true);
        }
    }

    private void handleHealthRegain(PacketEvent event, SpartanPlayer p) {
        p.getExecutor(Enums.HackType.FastHeal).handle(event.isCancelled(), event);

        if (p.getExecutor(Enums.HackType.FastHeal).prevent()) {
            event.setCancelled(true);
        }
    }
}