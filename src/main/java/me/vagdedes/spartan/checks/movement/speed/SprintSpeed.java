package me.vagdedes.spartan.checks.movement.speed;

import me.vagdedes.spartan.abstraction.CheckExecutor;
import me.vagdedes.spartan.abstraction.DetectionExecutor;
import me.vagdedes.spartan.compatibility.manual.abilities.crackshot.CrackShot;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.functionality.protections.LagLeniencies;
import me.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Damage;
import me.vagdedes.spartan.handlers.stability.Moderation;
import me.vagdedes.spartan.handlers.tracking.MovementProcessing;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.utils.gameplay.CombatUtils;
import me.vagdedes.spartan.utils.gameplay.PlayerData;

import java.util.LinkedHashMap;
import java.util.Map;

public class SprintSpeed extends DetectionExecutor {

    private final Speed speed;

    SprintSpeed(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
        speed = (Speed) executor;
    }

    private static final Direction[] directions = new Direction[]{
            new Direction("backwards", 0.0f, false),
            new Direction("sideways-1", 90.0f, true),
            new Direction("sideways-2", -90.0f, true),
            new Direction("diagonal-1", 135.0f, true),
            new Direction("diagonal-2", -135.0f, true)
    };
    private final Map<Integer, SpartanLocation> locationMemory = new LinkedHashMap<>(directions.length);
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

    void setLimit(double limit) {
        player.getDecimals(hackType).set(key + "limit", limit);
    }

    // Separator

    void run() {
        if (speed.player.getNmsHorizontalDistance() >= 0.25
                && !Moderation.wasDetected(speed.player)
                && !speed.player.wasInLiquids()
                && !LagLeniencies.hasHighLatency(speed.player, LagLeniencies.maxAllowedPing) // Library
                && PlayerData.getWalkSpeedDifference(speed.player) != 0.0f) {
            SpartanLocation from = speed.player.getEventFromLocation();
            Buffer buffer = speed.player.getBuffer(hackType);
            checkFoodSprint(from, buffer);
            checkOmnidirectional(speed.player.getEventToLocation(), from, buffer);
        }
    }

    // Separator

    private void checkFoodSprint(SpartanLocation from, Buffer buffer) {
        if (!hackType.getCheck().getBooleanOption("check_food_sprinting", true) || player.isBedrockPlayer()) {
            return;
        }
        runAsync(() -> {
            int attempts;
            boolean vanillaSprinting = player.isSprinting(),
                    customSprinting = player.isCustomSprinting();

            if (customSprinting) {
                attempts = buffer.increase("sprint", 1);
            } else {
                buffer.remove("sprint");
                attempts = 0;
            }

            if (player.getFoodLevel() <= 6) {
                int cases = customSprinting && attempts >= 10 ? 2 :
                        vanillaSprinting ? 1 :
                                player.isSprintJumping() ? 3 : 0;

                if (cases != 0 && buffer.start("food=level", 15) >= 10) {
                    cancel("type: sprint(food-level), case: " + cases, from);
                }
            }
        });
    }

    private void checkOmnidirectional(SpartanLocation to, SpartanLocation from, Buffer buffer) {
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
            double limit = player.getDecimals(hackType).get(key + "limit", MovementProcessing.sprintingMaxSpeed);
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
                            cancel("type: sprint(omnidirectional)"
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
                locationMemory.put(hash, to.clone().add(
                        CombatUtils.getDirection(yaw + direction.yaw, 0.0f).multiply(direction.plus ? limit : -limit)
                ));
            }
        });
    }
}