package me.vagdedes.spartan.checks.implementation.movement.speed;

import me.vagdedes.spartan.compatibility.manual.abilities.crackshot.CrackShot;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.features.performance.AsyncDetection;
import me.vagdedes.spartan.features.protections.LagLeniencies;
import me.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Damage;
import me.vagdedes.spartan.handlers.stability.Moderation;
import me.vagdedes.spartan.handlers.tracking.MovementProcessing;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.HackPrevention;
import me.vagdedes.spartan.system.Cache;
import me.vagdedes.spartan.utils.gameplay.CombatUtils;
import me.vagdedes.spartan.utils.gameplay.PlayerData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class SprintSpeed {

    private static final Map<UUID, Map<String, SpartanLocation>> locationMemory = Cache.store(new LinkedHashMap<>(Config.getMaxPlayers()));
    private static final Direction[] directions = new Direction[]{
            new Direction("backwards", 0.0f, false),
            new Direction("sideways-1", 90.0f, true),
            new Direction("sideways-2", -90.0f, true),
            new Direction("diagonal-1", 135.0f, true),
            new Direction("diagonal-2", -135.0f, true)
    };
    private static final double distanceRequirement = 0.01;
    private static final String key = "sprint=";

    // Separator

    private static class Direction {

        private final String name;
        private final float yaw;
        private final boolean plus;

        public Direction(String name, float yaw, boolean plus) {
            this.name = name;
            this.yaw = yaw;
            this.plus = plus;
        }
    }

    // Separator

    static void remove(SpartanPlayer p) {
        locationMemory.remove(p.getUniqueId());
    }

    static void setLimit(SpartanPlayer p, double limit) {
        p.getDecimals(Speed.check).set(key + "limit", limit);
    }

    // Separator

    public static void run(SpartanPlayer p, SpartanLocation to, SpartanLocation from, double ver, double hor) {
        if (hor >= 0.25 && canDo(p, to)) {
            Buffer buffer = p.getBuffer(Speed.check);
            checkFoodSprint(p, from, buffer);
            checkOmnidirectional(p, to, from, buffer, ver);
        }
    }

    // Separator

    private static void checkFoodSprint(SpartanPlayer p, SpartanLocation from, Buffer buffer) {
        if (!Speed.check.getCheck().getBooleanOption("check_food_sprinting", true) || p.isBedrockPlayer()) {
            return;
        }
        AsyncDetection.run(p, Speed.check, () -> {
            int attempts;
            boolean vanillaSprinting = p.isSprinting(),
                    customSprinting = p.isCustomSprinting();

            if (customSprinting) {
                attempts = buffer.increase("sprint", 1);
            } else {
                buffer.remove("sprint");
                attempts = 0;
            }

            if (p.getFoodLevel() <= 6) {
                int cases = customSprinting && attempts >= 10 ? 2 :
                        vanillaSprinting ? 1 :
                                p.isSprintJumping() ? 3 : 0;

                if (cases != 0 && buffer.start("food=level", 15) >= 10) {
                    new HackPrevention(p, Speed.check, "type: sprint(food-level), case: " + cases, from);
                }
            }
        });
    }

    private static void checkOmnidirectional(SpartanPlayer player, SpartanLocation to, SpartanLocation from, Buffer buffer, double dy) {
        if (!Speed.check.getCheck().getBooleanOption("check_omnidirectional_sprinting", true)
                || Damage.hasCooldown(player)
                || player.getHandlers().has(Handlers.HandlerType.Explosion)
                || player.getHandlers().has(Handlers.HandlerType.Piston)
                || CrackShot.isUsingScope(player)
                || Compatibility.CompatibilityType.MajorIncompatibility.isFunctional()
                || Compatibility.CompatibilityType.ItemAttributes.isFunctional()) {
            return;
        }
        AsyncDetection.run(player, Speed.check, () -> {
            double limit = player.getDecimals(Speed.check).get(key + "limit", MovementProcessing.sprintingMaxSpeed);
            float yaw = to.getYaw();
            boolean violated = false,
                    jumping = player.getLastJump() <= 1_000L;
            UUID uuid = player.getUniqueId();
            Map<String, SpartanLocation> locations = locationMemory.get(uuid);

            if (locations == null) {
                locations = new LinkedHashMap<>();
                locationMemory.put(uuid, locations);
            }
            for (Direction direction : directions) {
                if (!violated) {
                    SpartanLocation comparison = locations.get(direction.name);

                    if (comparison != null) {
                        double distance = to.distance(comparison);

                        if (distance <= distanceRequirement
                                && (!jumping || buffer.start(key + direction.name, 20) == 12)) {
                            violated = true;
                            new HackPrevention(player, Speed.check,
                                    "type: sprint(omnidirectional)"
                                            + ", direction: " + direction.name
                                            + ", yaw: " + yaw
                                            + ", yaw-modification: " + direction.yaw
                                            + ", positive-vector: " + direction.plus
                                            + ", limit: " + limit
                                            + ", distance: " + distance
                                            + ", distance-requirement: " + distanceRequirement, from
                            );
                        }
                    }
                }
                locations.put(direction.name, to.clone().add(
                        CombatUtils.getDirection(yaw + direction.yaw, 0.0f).multiply(direction.plus ? limit : -limit)
                ));
            }
        });
    }

    // Separator

    private static boolean canDo(SpartanPlayer p, SpartanLocation to) {
        return Speed.canDo(p, to)
                && !Moderation.isDetected(p)
                && !p.wasInLiquids()
                && !LagLeniencies.hasHighLatency(p, LagLeniencies.maxAllowedPing) // Library
                && PlayerData.getWalkSpeedDifference(p) != 0.0f;
    }
}