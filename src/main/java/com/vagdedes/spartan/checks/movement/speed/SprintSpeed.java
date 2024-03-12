package com.vagdedes.spartan.checks.movement.speed;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.data.Handlers;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.compatibility.manual.abilities.crackshot.CrackShot;
import com.vagdedes.spartan.functionality.identifiers.complex.unpredictable.Damage;
import com.vagdedes.spartan.functionality.identifiers.simple.DetectionLocation;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerData;

import java.util.LinkedHashMap;
import java.util.Map;

public class SprintSpeed extends DetectionExecutor {

    SprintSpeed(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    private static final Direction[] directions = new Direction[]{
            new Direction("backwards", 0.0f, false),
            new Direction("sideways-1", 90.0f, true),
            new Direction("sideways-2", -90.0f, true),
            new Direction("diagonal-1", 135.0f, true),
            new Direction("diagonal-2", -135.0f, true)
    };
    private final Map<Integer, SpartanLocation> locationMemory = new LinkedHashMap<>(directions.length);
    private static final double
            distanceRequirement = 0.01,
            sprintingMaxSpeed = 0.287;

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

    void setLimit(double limit) {
        player.getDecimals(hackType).set(key + "limit", limit);
    }

    // Separator

    void run() {
        if (!player.getHandlers().has(Handlers.HandlerType.Vehicle)
                && !player.wasInLiquids()
                && PlayerData.getWalkSpeedDifference(player) != 0.0f) {
            Double nmsHorizontalDistance = player.getNmsHorizontalDistance();

            if (nmsHorizontalDistance != null && nmsHorizontalDistance >= 0.25) {
                SpartanLocation from = player.getEventFromLocation();
                Buffer buffer = player.getBuffer(hackType);
                checkFoodSprint(buffer);
                checkOmnidirectional(player.getEventToLocation(), buffer);
            }
        }
    }

    // Separator

    private void checkFoodSprint(Buffer buffer) {
        if (!hackType.getCheck().getBooleanOption("check_food_sprinting", true) || player.bedrockPlayer) {
            return;
        }
        runAsync(() -> {
            if (player.getFoodLevel() <= 6) {
                int cases = player.isSprinting() ? 1 :
                        player.isSprintJumping() ? 3
                                : 0;

                if (cases != 0 && buffer.start("food=level", 15) >= 10) {
                    cancel(
                            "type: sprint(food-level), case: " + cases,
                            DetectionLocation.get(player, true)
                    );
                }
            }
        });
    }

    private void checkOmnidirectional(SpartanLocation to, Buffer buffer) {
        if (!hackType.getCheck().getBooleanOption("check_omnidirectional_sprinting", true)
                || Damage.hasCooldown(player)
                || player.getHandlers().has(Handlers.HandlerType.Explosion)
                || player.getHandlers().has(Handlers.HandlerType.Piston)
                || CrackShot.isUsingScope(player)
                || Compatibility.CompatibilityType.MajorIncompatibility.isFunctional()
                || Compatibility.CompatibilityType.ItemAttributes.isFunctional()) {
            return;
        }
        runAsync(() -> {
            double limit = player.getDecimals(hackType).get(key + "limit", sprintingMaxSpeed);
            float yaw = to.getYaw();
            boolean violated = false,
                    jumping = player.getLastJump() <= 1_000L;

            for (Direction direction : directions) {
                int hash = direction.hashCode();

                if (!violated) {
                    SpartanLocation comparison = locationMemory.get(hash);

                    if (comparison != null) {
                        double distance = to.distance(comparison);

                        if (distance <= distanceRequirement
                                && (!jumping || buffer.start(key + direction.name, 20) == 12)) {
                            violated = true;
                            cancel(
                                    "type: sprint(omnidirectional)"
                                    + ", direction: " + direction.name
                                    + ", yaw: " + yaw
                                    + ", yaw-modification: " + direction.yaw
                                    + ", positive-vector: " + direction.plus
                                    + ", limit: " + limit
                                    + ", distance: " + distance
                                    + ", distance-requirement: " + distanceRequirement,
                                    DetectionLocation.get(player, true)
                            );
                        }
                    }
                }
                locationMemory.put(hash, to.clone().add(
                        CombatUtils.getDirection(yaw + direction.yaw, 0.0f).multiply(direction.plus ? limit : -limit)
                ));
            }
        });
    }
}