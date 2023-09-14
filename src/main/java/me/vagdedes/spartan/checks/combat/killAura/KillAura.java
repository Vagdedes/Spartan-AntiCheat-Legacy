package me.vagdedes.spartan.checks.combat.killAura;

import me.vagdedes.spartan.compatibility.manual.abilities.mcmmo.mcMMO;
import me.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.features.performance.AsyncDetection;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.objects.system.HackPrevention;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.gameplay.CombatUtils;
import me.vagdedes.spartan.utils.gameplay.PlayerData;
import org.bukkit.entity.LivingEntity;

import java.util.UUID;

public class KillAura {

    static final Enums.HackType check = Enums.HackType.KillAura;
    private static Check checkObject = check.getCheck();
    static final double
            minimumDistance = 0.1,
            normalWidthToPlayerView = 0.7,
            universalNmsDistanceRequirement = 0.2;
    static final long pastDamageMilliseconds = 1_000L;

    public static void run(SpartanPlayer player, SpartanLocation location, LivingEntity entity,
                           double[] utility) {
        checkObject = check.getCheck();

        if (canRunDetection(player, null)
                && canRunEntity(player, entity)) {
            double distance = utility[2];

            if (distance >= minimumDistance) {
                double width = utility[0], height = utility[1];

                if (width != 0.0 && height != 0.0) {
                    boolean isFightParticipant = PlayerData.isFightParticipant(player);

                    runDetection(
                            player,
                            "check_irregular",
                            () -> Irregular.run(player),
                            true
                    );
                    runDetection(
                            player,
                            "check_raytrace",
                            () -> Raytrace.run(player, location, entity, distance),
                            true
                    );
                    runDetection(
                            player,
                            "check_distance",
                            () -> Distance.run(player, location, entity, distance, isFightParticipant),
                            true
                    );
                    runDetection(
                            player,
                            "check_time",
                            () -> Time.run(player, entity, width, distance),
                            true
                    );
                    runDetection(
                            player,
                            "check_rotations",
                            () -> Rotations.run(player, entity, width),
                            true
                    );
                    runDetection(
                            player,
                            "check_direction",
                            () -> Direction.run(player, entity, width, height, distance),
                            true
                    );
                    runDetection(
                            player,
                            "check_movement",
                            () -> Movement.run(player, entity, distance, width),
                            true
                    );
                    runDetection(
                            player,
                            "check_response",
                            () -> Response.run(player, entity, width, distance),
                            true
                    );
                    runDetection(
                            player,
                            "check_pattern",
                            () -> Pattern.run(player, entity, location),
                            true
                    );
                    runDetection(
                            player,
                            "check_aim",
                            () -> Aim.run(player, entity, location, width, height),
                            true
                    );
                }
            }
        }
    }

    public static void remove(SpartanPlayer player) {
        Aim.remove(player);
        Sensitivity.remove(player);
        Strafe.remove(player);
        Pattern.remove(player);
    }

    public static void runOnMovement(SpartanPlayer player, SpartanLocation to, SpartanLocation from) {
        if (canRunDetection(player, null)) {
            Buffer buffer = player.getBuffer(check);

            runDetection(
                    player,
                    "check_aim",
                    () -> Aim.runMove(player, to, from, buffer),
                    false
            );
            runDetection(
                    player,
                    "check_sensitivity",
                    () -> Sensitivity.run(player, to, buffer),
                    false
            );
            runDetection(
                    player,
                    "check_response",
                    () -> Response.runMove(player, buffer),
                    false
            );
            runDetection(
                    player,
                    "check_strafe",
                    () -> Strafe.run(player, to, buffer),
                    false
            );
            runDetection(
                    player,
                    "check_pattern",
                    () -> Pattern.runMove(player, from),
                    false
            );
        }
    }

    private static void runDetection(SpartanPlayer player, String option, Runnable runnable, boolean sync) {
        if (checkObject.getBooleanOption(option, true)) {
            if (sync) {
                runnable.run();
            } else {
                AsyncDetection.run(player, check, runnable);
            }
        }
    }

    static HackPrevention violate(SpartanPlayer player, String information) {
        return new HackPrevention(player, check, information);
    }

    static void violateLive(SpartanPlayer player, String information) {
        new HackPrevention(player, check, information, null, CombatUtils.getFightTicks(player));
        player.getViolations(check).process();
    }

    static void violateAll(SpartanPlayer player, String information) {
        new HackPrevention(player, check, information,
                null, 0, false,
                0.0, 2);
    }

    static boolean canRunDetection(SpartanPlayer player, String option) {
        return (option == null || check.getCheck().getBooleanOption(option, true))
                && (player == null
                || player.canRunCheck(check, true)
                && !player.wasFlying()
                && !player.getHandlers().has(Handlers.HandlerType.ElytraUse)
                && !mcMMO.hasAbility(player)
                && !Attributes.has(player, Attributes.GENERIC_ARMOR));
    }

    static boolean canRunEntity(SpartanPlayer player, LivingEntity entity) {
        if (entity.getVehicle() == null && !CombatUtils.isEntityWide(CombatUtils.getWidthAndHeight(entity)[0])) {
            UUID entityUUID = entity.getUniqueId();
            boolean isPlayer = SpartanBukkit.isPlayer(entityUUID);
            return (isPlayer || !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_10) || entity.hasAI())
                    && !player.getUniqueId().equals(entityUUID)
                    && checkObject.getBooleanOption("detection." + (isPlayer ? "players" : "entities"), true);
        }
        return false;
    }

    // Object Orientation


}
