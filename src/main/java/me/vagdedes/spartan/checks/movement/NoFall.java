package me.vagdedes.spartan.checks.movement;

import me.vagdedes.spartan.compatibility.manual.damage.NoHitDelay;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.features.performance.AsyncDetection;
import me.vagdedes.spartan.features.protections.Building;
import me.vagdedes.spartan.handlers.identifiers.BouncingBlocks;
import me.vagdedes.spartan.handlers.identifiers.FloorProtection;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.data.Cooldowns;
import me.vagdedes.spartan.objects.data.Decimals;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.hackPrevention.HackPrevention;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;
import me.vagdedes.spartan.utils.gameplay.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

public class NoFall {

    public static final HackType check = HackType.NoFall;
    private static final String ratioKey = "ratio=";

    // Utilities

    public static void handleDamage(SpartanPlayer p) {
        p.getCooldowns(check).add("land", 10);
    }

    private static void cancel(SpartanPlayer p, String message, SpartanLocation loc, int time, double damage) {
        new HackPrevention(p, check, message, loc, time, true, damage);
    }

    // Runnables

    public static void run(SpartanPlayer p, SpartanLocation to, SpartanLocation from,
                           double vertical, double oldVertical, double horizontal,
                           int airTicks) {
        float fallDistance = p.getFallDistance();

        if (fallDistance > 0.0f
                || airTicks > 0
                || horizontal >= 0.1
                || vertical != 0.0
                || oldVertical != 0.0) {
            Buffer buffer = p.getBuffer(check);
            Decimals decimals = p.getDecimals(check);

            if (canDo(p, to, buffer, decimals)) {
                AsyncDetection.run(p, check, () -> {
                    boolean requiresSuspicion = p.getProfile().isSuspectedOrHacker(check),
                            offGround = p.getBlocksOffGround(3, true, true) >= 2;

                    if (!BouncingBlocks.isBelow(p) && !BlockUtils.isBouncingBlock(p, to, 2)) {
                        if (requiresSuspicion) {
                            checkLanding(p, to, from, vertical, fallDistance);
                        }
                        manageRatio(p, to, from, vertical, airTicks, requiresSuspicion, buffer, decimals);
                    } else {
                        clearRatio(buffer, decimals);
                    }

                    if (offGround && vertical <= -0.1 && !BouncingBlocks.isBelow(p)) {
                        if (to.getBlockY() < from.getBlockY()) {
                            double oldFallDistance = decimals.get("old");

                            if (decimals.has(oldFallDistance)) {
                                float fallDistanceDifference = p.getFallDistance() - (float) oldFallDistance;
                                checkAir(p, to, from, fallDistanceDifference, (fallDistance - Math.floor(fallDistance)), vertical, airTicks, requiresSuspicion);

                                if (requiresSuspicion) {
                                    checkPattern(p, to, from, buffer, fallDistanceDifference, vertical, airTicks);
                                }
                            }
                        }
                        if (!Compatibility.CompatibilityType.MajorIncompatibility.isFunctional()) {
                            checkSpoof(p, to, from, vertical, airTicks);
                        }
                    }
                });
            }
        }
        p.getDecimals(check).set("old", fallDistance);
    }

    // Air

    private static void checkAir(SpartanPlayer p, SpartanLocation to, SpartanLocation from,
                                 float fallDistanceDifference,
                                 double fallDistanceRemaining, double vertical,
                                 int airTicks,
                                 boolean requiresSuspicion) {
        if (check.getCheck().getBooleanOption("check_air", true)) {
            String cooldownKey = "air=cooldown";
            int cooldownTicks = 10;

            if (airTicks > 0 && vertical > -3.5 && !p.getHandlers().has(Handlers.HandlerType.ElytraWear) && !PlayerData.hasJumpEffect(p)) {
                for (int i = -1; i <= 1; i++) {
                    if (!BlockUtils.isNonSolidFree(to, BlockUtils.hitbox_max, 1, BlockUtils.hitbox_max)) {
                        p.getCooldowns(check).add(cooldownKey, cooldownTicks);
                        return;
                    }
                }
                if (!p.getCooldowns(check).canDo(cooldownKey)) {
                    return;
                }

                if (fallDistanceDifference <= 0.0f && fallDistanceRemaining == 0.0 && (requiresSuspicion || airTicks >= 8 || vertical <= -0.5)) {
                    double ratio = p.getCalculatedFallingRatio(vertical);

                    if (ratio <= 0.5) {
                        cancel(p, "type: air, fall-distance: " + fallDistanceDifference + ", air: " + airTicks + ", distance: " + vertical + ", ratio: " + ratio, from, 5, 2.0);
                    }
                }
            } else {
                p.getCooldowns(check).add(cooldownKey, cooldownTicks);
            }
        }
    }

    // Pattern

    private static void checkPattern(SpartanPlayer p, SpartanLocation to, SpartanLocation from,
                                     Buffer buffer,
                                     float fallDistanceDifference,
                                     double vertical,
                                     int airTicks) {
        if (check.getCheck().getBooleanOption("check_pattern", true)
                && !p.isOnGroundCustom()
                && !PlayerData.isOnGround(p, to, 0, -1, 0)
                && !PlayerData.hasJumpEffect(p)) {
            String key = "pattern";

            if (fallDistanceDifference > 0.0f) {
                buffer.increase(key, 1);
            } else {
                int pattern = buffer.get(key);

                if (pattern > 1) {
                    int cachedPattern = buffer.get(key + "=cache", Integer.MIN_VALUE);

                    if (cachedPattern != Integer.MIN_VALUE && cachedPattern == pattern) {
                        cancel(p, "type: pattern, fall-distance-difference: " + fallDistanceDifference + ", air: " + airTicks + ", distance: " + vertical, from, 5, 2.0);
                    }
                    buffer.set(key + "=cache", pattern);
                }
                buffer.remove(key);
            }
        }
    }

    // Landing

    private static void checkLanding(SpartanPlayer p, SpartanLocation to, SpartanLocation loc, double vertical, float fall) {
        if (check.getCheck().getBooleanOption("check_landing", true)
                && !PlayerData.hasSpeedEffect(p, to) && !PlayerData.hasJumpEffect(p)) {
            String key = "land";

            if (vertical < 0.0) {
                if (fall >= 6.0f) {
                    p.getCooldowns(check).add(key, 10);
                }
            } else {
                Cooldowns cooldowns = p.getCooldowns(check);

                if (!cooldowns.canDo(key)) {
                    cooldowns.remove(key);

                    if (!p.isOnGround() && p.isOnGroundCustom()
                            && BlockUtils.isFullSolid(to.clone().add(0, -1, 0), true)
                            && BlockUtils.isSolidFree(to, BlockUtils.hitbox, 0, BlockUtils.hitbox)) {
                        cancel(p, "type: land", loc, 10, 5.0);
                    }
                }
            }
        }
    }

    // Spoof

    private static void checkSpoof(SpartanPlayer p, SpartanLocation to, SpartanLocation from,
                                   double vertical,
                                   int airTicks) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8) && check.getCheck().getBooleanOption("check_spoof", true)
                && (airTicks >= 20 || airTicks >= 12 && p.isFalling(vertical))
                && p.isOnGround() && !p.isOnGroundCustom()
                && !p.getHandlers().has(Handlers.HandlerType.ElytraWear) && p.isGroundSpoofing()) {
            boolean valid = true;

            for (int i = -1; i <= 2; i++) {
                if (!BlockUtils.isSolidFree(to, BlockUtils.hitbox, i, BlockUtils.hitbox)) {
                    valid = false;
                    break;
                }
            }

            if (valid) {
                cancel(p, "type: spoof, air: " + airTicks + ", vertical: " + vertical, from, 10, 0.0);
            }
        }
    }

    // Ratio

    private static void manageRatio(SpartanPlayer p, SpartanLocation to, SpartanLocation from,
                                    double vertical,
                                    int airTicks,
                                    boolean requiresSuspicion,
                                    Buffer buffer, Decimals decimals) {
        if (!p.getHandlers().has(Handlers.HandlerType.BlockBreak)) {
            if (check.getCheck().getBooleanOption("check_ratio", true) && !PlayerData.hasJumpEffect(p)) {
                if (buffer.get(ratioKey + "cancelled") != 1) {
                    int toY = to.getBlockY(), fromY = from.getBlockY();

                    if (toY < fromY) {
                        int blockY = buffer.get(ratioKey + "block-y", Integer.MIN_VALUE);

                        if (blockY != Integer.MIN_VALUE && blockY != toY) {
                            buffer.increase(ratioKey + "blocks", 1);
                        }
                        buffer.set(ratioKey + "block-y", toY);

                        if (vertical < -(MoveUtils.falling[0] * 8) && airTicks > 8) {
                            buffer.set(ratioKey + "validated", 1);
                        }
                    } else if (buffer.get(ratioKey + "validated") == 1 && Math.abs(toY - fromY) <= 3.0 && to.distance(from) <= MoveUtils.chunk) {
                        if (!p.isJumping(vertical)) {
                            int cases, bufferTicks = 5, bufferRequirement = 2;

                            if (p.isOnGroundCustom() && (!p.isBedrockPlayer() || buffer.start(ratioKey + "ground", bufferTicks) >= bufferRequirement)) { // Attention: p.isOnGroundCustom() replace isOnGround(to, 0, 0, 0) recently
                                cases = 1;
                            } else if (vertical == 0.0 && (requiresSuspicion || buffer.start(ratioKey + "movement", bufferTicks) >= bufferRequirement)) {
                                buffer.remove(ratioKey + "movement");
                                cases = 2;
                            } else if (vertical > 0.0) {
                                cases = 3;
                            } else if (airTicks <= 5) {
                                cases = 4;
                            } else {
                                cases = 0;
                            }

                            if (cases != 0) {
                                SpartanBukkit.runDelayedTask(p, () -> {
                                    if (p != null) {
                                        if (BlockUtils.isNonSolidFree(to, BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max)
                                                && BlockUtils.isNonSolidFree(to, BlockUtils.hitbox_max, -1, BlockUtils.hitbox_max)) {
                                            int blocks = buffer.get(ratioKey + "blocks");

                                            // bigger or "equal" to 3 due to abstract counting of blocks
                                            if (blocks >= MoveUtils.fallDamageBlocks && !decimals.has(decimals.get(ratioKey + "damage"))) {
                                                boolean safe = blocks >= (MoveUtils.fallDamageBlocks + 1);
                                                cancel(p, "type: ratio, blocks: " + blocks + ", case: " + cases, to, safe ? 10 : 0, safe ? blocks : 0.0);
                                            }
                                        }
                                        clearRatio(buffer, decimals);
                                    }
                                }, 2L);
                            }
                        }
                    } else {
                        decimals.remove(ratioKey + "damage");
                    }
                } else {
                    clearRatio(buffer, decimals);
                }
            } else {
                clearRatio(buffer, decimals);
            }
        }
    }

    public static void manageRatio(SpartanPlayer p, EntityDamageEvent.DamageCause cause,
                                   double dmg, boolean cancelled) {
        String s = "ratio=";

        if (!check.getCheck().getBooleanOption("check_ratio", true) || PlayerData.hasJumpEffect(p) || cause != EntityDamageEvent.DamageCause.FALL) {
            clearRatio(p.getBuffer(check), p.getDecimals(check));
        } else if (cancelled) {
            Buffer buffer = p.getBuffer(check);
            clearRatio(buffer, p.getDecimals(check));
            buffer.set(s + "cancelled", 1);
        } else {
            p.getDecimals(check).set(s + "damage", dmg);
        }
    }

    private static void clearRatio(Buffer buffer, Decimals decimals) {
        buffer.remove(new String[]{ratioKey + "blocks", ratioKey + "block-y", ratioKey + "cancelled", ratioKey + "validated", ratioKey + "movement"});
        decimals.remove(ratioKey + "damage");
    }

    // Logic

    private static boolean canDo(SpartanPlayer p, SpartanLocation loc,
                                 Buffer buffer, Decimals decimals) {
        if (p.canRunCheck(check, true)) {
            GameMode gameMode = p.getGameMode();

            if (gameMode == GameMode.SURVIVAL || gameMode == GameMode.SURVIVAL.ADVENTURE) {
                EntityDamageEvent damageCause = p.getLastDamageCause();

                if ((damageCause.getCause() != EntityDamageEvent.DamageCause.FALL || !damageCause.isCancelled())
                        && !p.isInvulnerable()
                        && !p.wasInLiquids()
                        && loc.getBlockY() > BlockUtils.getMinHeight(p.getWorld())
                        && !p.getHandlers().has(Handlers.HandlerType.ElytraUse)
                        && !p.getHandlers().has(Handlers.HandlerType.Vehicle)
                        && !p.getHandlers().has(Handlers.HandlerType.WaterElevator)
                        && !p.getHandlers().has(Handlers.HandlerType.Explosion)
                        && !NoHitDelay.hasCooldown(p)
                        && !Building.hasCooldown(p)
                        && !PlayerData.hasLevitationEffect(p)
                        && !PlayerData.hasSlowFallingEffect(p)
                        && !FloorProtection.hasCooldown(p, true)
                        && !BlockUtils.canClimb(loc)
                        && !p.stepsOnBoats()
                        && (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15) || p.getWorld().getGameRuleValue(GameRule.FALL_DAMAGE) == true)) {
                    ItemStack boots = p.getInventory().getBoots();

                    if (boots == null || !boots.containsEnchantment(Enchantment.PROTECTION_FALL)) {
                        for (int i = -1; i <= 2; i++) {
                            if (!BlockUtils.isWebFree(loc, BlockUtils.hitbox_max, i, BlockUtils.hitbox_max)) {
                                clearRatio(buffer, decimals);
                                return false;
                            }
                        }
                        return true;
                    }
                }
            }
        }
        clearRatio(buffer, decimals);
        return false;
    }
}
