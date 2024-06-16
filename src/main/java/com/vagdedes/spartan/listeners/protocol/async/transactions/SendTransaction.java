package com.vagdedes.spartan.listeners.protocol.async.transactions;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SendTransaction extends PacketAdapter {

    private static final short def = 1488;
    private static final Map<UUID, Short> id = new ConcurrentHashMap<>();

    public SendTransaction() {
        super(
                Register.plugin,
                ListenerPriority.HIGHEST,
                PacketType.Play.Client.TRANSACTION
        );
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        Player player = event.getPlayer();
        short current = id.getOrDefault(player.getUniqueId(), def);

        if (Objects.equals(packet.getShorts().read(0), current)) {
            current++;

            if (current > 1500) {
                id.put(player.getUniqueId(), current = def);
            } else {
                id.put(player.getUniqueId(), current);
            }
            SpartanBukkit.getProtocol(player).setLastTransaction();
            PacketContainer transaction = new PacketContainer(PacketType.Play.Server.TRANSACTION);
            transaction.getShorts().write(0, current);
            SpartanBukkit.transferTask(
                    player,
                    () -> ProtocolLibrary.getProtocolManager().sendServerPacket(player, transaction)
            );
        }
    }

}
