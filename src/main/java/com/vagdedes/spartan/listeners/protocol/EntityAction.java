package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.player.packets.Container;
import com.vagdedes.spartan.abstraction.player.packets.IPlayer;
import com.vagdedes.spartan.listeners.protocol.modules.AbilitiesEnum;
import org.bukkit.entity.Player;

public class EntityAction extends PacketAdapter {

    public EntityAction() {
        super(Register.plugin, ListenerPriority.HIGHEST, PacketType.Play.Client.ENTITY_ACTION);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {

        Player player = event.getPlayer();
        IPlayer iPlayer = Container.getIPlayer(player);

        String typeString = event.getPacket().getModifier().getValues().get(1).toString();
        AbilitiesEnum type = getEnum(typeString);
        if (typeString != null) {
            if (type == AbilitiesEnum.PRESS_SHIFT_KEY) {
                iPlayer.setSneaking(true);
            }
            if (type == AbilitiesEnum.RELEASE_SHIFT_KEY) {
                iPlayer.setSneaking(false);
            }
            if (type == AbilitiesEnum.START_SPRINTING) {
                iPlayer.setSprinting(true);
            }
            if (type == AbilitiesEnum.STOP_SNEAKING) {
                iPlayer.setSprinting(false);
            }
        }

    }

    private AbilitiesEnum getEnum(String s) {
        for (AbilitiesEnum type : AbilitiesEnum.values()) {
            if (type.toString().equals(s)) {
                return type;
            }
        }
        return null;
    }

}
