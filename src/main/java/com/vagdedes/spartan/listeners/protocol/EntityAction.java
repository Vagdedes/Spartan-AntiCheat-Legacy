package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.protocol.modules.AbilitiesContainer;
import com.vagdedes.spartan.listeners.protocol.modules.AbilitiesEnum;

public class EntityAction extends PacketAdapter {

    public EntityAction() {
        super(Register.plugin, ListenerPriority.HIGHEST, PacketType.Play.Client.ENTITY_ACTION);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        AbilitiesEnum type = AbilitiesEnum.valueOf(event.getPacket().getModifier().getValues().get(1).toString());
        AbilitiesContainer container = SpartanBukkit.getProtocol(event.getPlayer()).abilities;
        if (type == AbilitiesEnum.START_SPRINTING) container.setSprinting(true);
        if (type == AbilitiesEnum.STOP_SPRINTING) container.setSprinting(false);
        if (type == AbilitiesEnum.PRESS_SHIFT_KEY) container.setSneaking(true);
        if (type == AbilitiesEnum.RELEASE_SHIFT_KEY) container.setSneaking(false);
    }

}
