package com.vagdedes.spartan;

import com.vagdedes.spartan.functionality.command.CommandExecution;
import com.vagdedes.spartan.functionality.command.CommandTab;
import com.vagdedes.spartan.functionality.connection.cloud.JarVerification;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.npc.NPCManager;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.bukkit.*;
import com.vagdedes.spartan.listeners.bukkit.standalone.*;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.server.ProxyUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashSet;
import java.util.Set;

public class Register extends JavaPlugin {

    public static Plugin plugin = null;
    public static final String pluginName = "Spartan";
    public static final String command = "spartan";

    private static final Set<Class<?>> listeners = new LinkedHashSet<>(2);

    public void onEnable() {
        plugin = this;

        if (MultiVersion.serverVersion == MultiVersion.MCVersion.OTHER) {
            AwarenessNotifications.forcefullySend(
                    "The server's version or type is not supported. "
                            + "Please contact the plugin's developer if you think this is an error."
            );
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        JarVerification.run(this);
        Config.create();

        // Base
        Config.settings.runOnLogin();

        // Listeners (More may be registered elsewhere)
        enable(new Event_Join());
        enable(new Event_Leave());
        enable(new Event_Chat());
        enable(new Event_Plugin());
        enable(new Event_Inventory());
        enable(new Event_Bow());
        enable(new Event_Chunks());
        enable(new Event_VehicleDeath());
        enable(new Event_Health());
        enable(new Event_Combat());
        enable(new Event_BlockPlace());
        enable(new Event_Movement());
        enable(new Event_Teleport());
        enable(new Event_Death());
        enable(new Event_Vehicle());
        enable(new Event_Velocity());
        enable(new Event_World());
        enable(new Event_Damaged());

        //enable(new GhostBlockTest());

        if (NPCManager.supported) {
            enable(new NPCManager());
        }
        if (PlayerUtils.elytra) {
            enable(new Event_Elytra());

            if (PlayerUtils.trident) {
                enable(new Event_Trident());
            }
        }
        ProxyUtils.register();

        // Commands
        this.getCommand(command).setExecutor(new CommandExecution());
        this.getCommand(command).setTabCompleter(new CommandTab());
    }

    public void onDisable() {
        plugin = this;

        // Separator
        ProxyUtils.unregister();
        listeners.clear();
        SpartanBukkit.disable();
    }

    // Utilities

    public static void enable(Listener l) {
        if (isPluginEnabled() && listeners.add(l.getClass())) {
            Bukkit.getPluginManager().registerEvents(l, plugin);
        }
    }

    public static boolean isPluginEnabled() {
        return plugin != null && plugin.isEnabled();
    }

}
