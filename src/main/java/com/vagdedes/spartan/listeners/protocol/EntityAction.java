package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.AntiCheatLogs;
import com.vagdedes.spartan.listeners.protocol.modules.AbilitiesContainer;
import com.vagdedes.spartan.listeners.protocol.modules.AbilitiesEnum;
import org.bukkit.Bukkit;

import java.util.Random;

public class EntityAction extends PacketAdapter {

    public EntityAction() {
        super(Register.plugin, ListenerPriority.HIGHEST, PacketType.Play.Client.ENTITY_ACTION);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        SpartanPlayer player = SpartanBukkit.getPlayer(event.getPlayer());

        if (player == null) {
            return;
        }

        AbilitiesEnum type = AbilitiesEnum.valueOf(event.getPacket().getModifier().getValues().get(1).toString());
        AbilitiesContainer container = ProtocolStorage.getAbilities(player.uuid);
        if (type == AbilitiesEnum.START_SPRINTING) container.setSprint(true);
        if (type == AbilitiesEnum.STOP_SPRINTING) container.setSprint(false);
        if (type == AbilitiesEnum.PRESS_SHIFT_KEY) container.setSneak(true);
        if (type == AbilitiesEnum.RELEASE_SHIFT_KEY) container.setSneak(false);
    }

}
