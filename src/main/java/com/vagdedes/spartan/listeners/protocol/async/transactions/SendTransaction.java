package com.vagdedes.spartan.listeners.protocol.async.transactions;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class SendTransaction extends PacketAdapter {

    private static Map<Player, Short> id = new ConcurrentHashMap<>();
    private static Map<Player, Long> delay = new ConcurrentHashMap<>();
    private static Map<Player, Integer> ping = new ConcurrentHashMap<>();
    private static boolean isModernVersion;

    public SendTransaction() {
        super(
                        Register.plugin,
                        ListenerPriority.HIGHEST,
                        PacketType.Play.Client.TRANSACTION
        );

        isModernVersion = (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17));
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        Player player = event.getPlayer();
        if (Objects.equals(packet.getShorts().read(0), id.get(player))) {
            id.put(player, (short) (id.get(player) + 1));
            if (id.get(player) > 1500) id.put(player, (short) 1488);
            ping.put(player, (int) (System.currentTimeMillis() - delay.get(player)));
            sendTransaction(player);
        }
    }

    public static void sendTransaction(Player player) {
        ProtocolManager p = ProtocolLibrary.getProtocolManager();
        PacketContainer transaction;

        if (isModernVersion) {
            ping.put(player, player.getPing());
            SpartanBukkit.runDelayedTask(() -> sendTransaction(player), 500);
        } else {
            delay.put(player, System.currentTimeMillis());
            transaction = new PacketContainer(PacketType.Play.Server.TRANSACTION);
            transaction.getShorts().write(0, id.get(player));
            SpartanBukkit.transferTask(player, () -> p.sendServerPacket(player, transaction));
        }
    }

    public static void initializeTransaction(Player player) {
        id.put(player, (short) 1488);
        ping.put(player, 0);
        sendTransaction(player);
    }

    public static Integer getTransactionDelay(Player player) {
        return ping.get(player);
    }
}
