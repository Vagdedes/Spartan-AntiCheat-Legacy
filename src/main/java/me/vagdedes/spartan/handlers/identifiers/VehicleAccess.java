package me.vagdedes.spartan.handlers.identifiers;

import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LeashHitch;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class VehicleAccess {

    private static final int ticks = 7;

    public static void run() {
        List<SpartanPlayer> players = SpartanBukkit.getPlayers();

        if (players.size() > 0) {
            for (SpartanPlayer p : players) {
                Entity vehicle = p.getVehicle();

                if (vehicle != null && !(vehicle instanceof LeashHitch)) {
                    runEnter(p, vehicle);
                }
            }
        }
    }

    public static void runExit(SpartanPlayer p) {
        Entity vehicle = p.getVehicle();

        if (vehicle == null || vehicle instanceof LivingEntity) {
            Handlers handlers = p.getHandlers();
            handlers.add(Handlers.HandlerType.Vehicle, ticks);
            handlers.add(Handlers.HandlerType.Vehicle, "exit", ticks);

            if (handlers.has(Handlers.HandlerType.Vehicle, "non-boat")) {
                handlers.add(Handlers.HandlerType.Vehicle, "non-boat", ticks);
            }
        }
    }

    public static void runEnter(SpartanPlayer p, Entity entity) {
        Handlers handlers = p.getHandlers();
        handlers.add(Handlers.HandlerType.Vehicle);
        handlers.add(Handlers.HandlerType.Vehicle, "enter", ticks);

        if (!(entity instanceof Boat)) {
            handlers.add(Handlers.HandlerType.Vehicle, "non-boat");
        }
    }

    public static boolean hasExitCooldown(SpartanPlayer p, Enums.HackType hackType) {
        return hackType != Enums.HackType.EntityMove
                && p.getHandlers().has(Handlers.HandlerType.Vehicle, "exit")
                && !p.getProfile().isSuspectedOrHacker(hackType);
    }

    public static boolean hasEnterCooldown(SpartanPlayer p, Enums.HackType hackType) {
        return hackType != Enums.HackType.EntityMove
                && p.getHandlers().has(Handlers.HandlerType.Vehicle, "enter");
    }
}
