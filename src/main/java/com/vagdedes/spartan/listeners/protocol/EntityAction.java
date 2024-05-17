package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.listeners.protocol.modules.AbilitiesContainer;
import com.vagdedes.spartan.listeners.protocol.modules.AbilitiesEnum;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerKickEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EntityAction extends PacketAdapter {

    public static Map<Player, AbilitiesContainer> playerAbilitiesContainer = new ConcurrentHashMap<>();
    public EntityAction() {
        super(Register.plugin, ListenerPriority.HIGHEST, PacketType.Play.Client.ENTITY_ACTION);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        AbilitiesEnum type = AbilitiesEnum.valueOf(event.getPacket().getModifier().getValues().get(1).toString());
        if (type == AbilitiesEnum.START_SPRINTING) {
            if (getAbilities(player).sprint()) {
                getPlugin().getLogger().warning(player.getName()
                                + " use impossible EntityAction! (START_SPRINTING while already sprinting)");
                Bukkit.getScheduler().runTask(getPlugin(),
                                () -> player.kick(Component.text("Invalid packet received"),
                                                PlayerKickEvent.Cause.INVALID_PLAYER_MOVEMENT));
            } else {
                playerAbilitiesContainer.get(player).setSprint(true);
            }
        }
        if (type == AbilitiesEnum.STOP_SPRINTING) {
            if (!getAbilities(player).sprint()) {
                getPlugin().getLogger().warning(player.getName()
                                + " use impossible EntityAction! (STOP_SPRINTING while already non-sprinting)");
                Bukkit.getScheduler().runTask(getPlugin(),
                                () -> player.kick(Component.text("Invalid packet received"),
                                                PlayerKickEvent.Cause.INVALID_PLAYER_MOVEMENT));
            } else {
                playerAbilitiesContainer.get(player).setSprint(false);
            }
        }
        if (type == AbilitiesEnum.PRESS_SHIFT_KEY) {
            if (getAbilities(player).sneak()) {
                getPlugin().getLogger().warning(player.getName()
                                + " use impossible EntityAction! (PRESS_SHIFT_KEY while already sneaking)");
                Bukkit.getScheduler().runTask(getPlugin(),
                                () -> player.kick(Component.text("Invalid packet received"),
                                                PlayerKickEvent.Cause.INVALID_PLAYER_MOVEMENT));
            } else {
                playerAbilitiesContainer.get(player).setSneak(true);
            }
        }
        if (type == AbilitiesEnum.RELEASE_SHIFT_KEY) {
            if (!getAbilities(player).sneak()) {
                getPlugin().getLogger().warning(player.getName()
                                + " use impossible EntityAction! (RELEASE_SHIFT_KEY while already non-sneaking)");
                Bukkit.getScheduler().runTask(getPlugin(),
                                () -> player.kick(Component.text("Invalid packet received"),
                                                PlayerKickEvent.Cause.INVALID_PLAYER_MOVEMENT));
            } else {
                playerAbilitiesContainer.get(player).setSneak(false);
            }
        }
    }

    public static AbilitiesContainer getAbilities(Player player) {
        return playerAbilitiesContainer.get(player);
    }
    public static AbilitiesContainer getAbilities(SpartanPlayer player) {
        return playerAbilitiesContainer.get(player.getInstance());
    }

}
