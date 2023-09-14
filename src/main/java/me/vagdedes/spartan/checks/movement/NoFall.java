package me.vagdedes.spartan.checks.movement;

import me.vagdedes.spartan.abstraction.CheckExecutor;
import me.vagdedes.spartan.compatibility.manual.damage.NoHitDelay;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.features.performance.AsyncDetection;
import me.vagdedes.spartan.features.protections.Building;
import me.vagdedes.spartan.handlers.identifiers.complex.predictable.BouncingBlocks;
import me.vagdedes.spartan.handlers.identifiers.complex.predictable.FloorProtection;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.data.Cooldowns;
import me.vagdedes.spartan.objects.data.Decimals;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.HackPrevention;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;
import me.vagdedes.spartan.utils.gameplay.PatternUtils;
import me.vagdedes.spartan.utils.gameplay.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

public class NoFall extends CheckExecutor {

    public static final HackType check = HackType.NoFall;
    private static final String ratioKey = "ratio=";

    private static final double[][]
            air = new double[][]{
            {BlockUtils.hitbox_max, -1, BlockUtils.hitbox_max},
            {BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max},
            {BlockUtils.hitbox_max, 1, BlockUtils.hitbox_max}
    }, spoof = new double[][]{
            {BlockUtils.hitbox, -1, BlockUtils.hitbox},
            {BlockUtils.hitbox, 0, BlockUtils.hitbox},
            {BlockUtils.hitbox, 1, BlockUtils.hitbox},
            {BlockUtils.hitbox, 2, BlockUtils.hitbox}
    }, ratio = new double[][]{
            {BlockUtils.hitbox_max, -1, BlockUtils.hitbox_max},
            {BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max},
    }, canDo = new double[][]{
            {BlockUtils.hitbox_max, -1, BlockUtils.hitbox_max},
            {BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max},
            {BlockUtils.hitbox_max, 1, BlockUtils.hitbox_max},
            {BlockUtils.hitbox_max, 2, BlockUtils.hitbox_max}
    };
    private static final double[]
            genericHitbox = new double[]{
            BlockUtils.hitbox, 0, BlockUtils.hitbox
    };

    // Air

    private void checkAir(SpartanLocation to, SpartanLocation from,
                          float fallDistanceDifference,
                          double fallDistanceRemaining, double vertical,
                          int airTicks,
                          boolean requiresSuspicion) {
        if (check.getCheck().getBooleanOption("check_air", true)) {
            String cooldownKey = "air=cooldown";
            int cooldownTicks = 10;

            if (airTicks > 0 && vertical > -3.5 && !player.getHandlers().has(Handlers.HandlerType.ElytraWear) && !PlayerData.hasJumpEffect(player)) {
                if (PatternUtils.isBlockPattern(air, to, true, BlockUtils.semi_solid)) {
                    player.getCooldowns(check).add(cooldownKey, cooldownTicks);
                    return;
                }
                if (!player.getCooldowns(check).canDo(cooldownKey)) {
                    return;
                }

                if (fallDistanceDifference <= 0.0f && fallDistanceRemaining == 0.0 && (requiresSuspicion || airTicks >= 8 || vertical <= -0.5)) {
                    double ratio = player.getCalculatedFallingRatio(vertical);

                    if (ratio <= 0.5) {
                        cancel("type: air, fall-distance: " + fallDistanceDifference + ", air: " + airTicks + ", distance: " + vertical + ", ratio: " + ratio, from, 5, true, 2.0);
                    }
                }
            } else {
                player.getCooldowns(check).add(cooldownKey, cooldownTicks);
            }
        }
    }

    // Pattern

    private void checkPattern(SpartanLocation to, SpartanLocation from,
                              Buffer buffer,
                              float fallDistanceDifference,
                              double vertical,
                              int airTicks) {
        if (check.getCheck().getBooleanOption("check_pattern", true)
                && !player.isOnGroundCustom()
                && !PlayerData.isOnGround(player, to, 0, -1, 0)
                && !PlayerData.hasJumpEffect(player)) {
            String key = "pattern";

            if (fallDistanceDifference > 0.0f) {
                buffer.increase(key, 1);
            } else {
                int pattern = buffer.get(key);

                if (pattern > 1) {
                    int cachedPattern = buffer.get(key + "=cache", Integer.MIN_VALUE);

                    if (cachedPattern != Integer.MIN_VALUE && cachedPattern == pattern) {
                        cancel("type: pattern, fall-distance-difference: " + fallDistanceDifference + ", air: " + airTicks + ", distance: " + vertical, from, 5, true, 2.0);
                    }
                    buffer.set(key + "=cache", pattern);
                }
                buffer.remove(key);
            }
        }
    }

    // Landing

    private void checkLanding(SpartanLocation to, SpartanLocation loc, double vertical, float fall) {
        if (check.getCheck().getBooleanOption("check_landing", true)
                && !PlayerData.hasSpeedEffect(player, to) && !PlayerData.hasJumpEffect(player)) {
            String key = "land";

            if (vertical < 0.0) {
                if (fall >= 6.0f) {
                    player.getCooldowns(check).add(key, 10);
                }
            } else {
                Cooldowns cooldowns = player.getCooldowns(check);

                if (!cooldowns.canDo(key)) {
                    cooldowns.remove(key);

                    if (!player.isOnGround() && player.isOnGroundCustom()
                            && BlockUtils.isFullSolid(to.clone().add(0, -1, 0))
                            && !PatternUtils.isBlockPattern(genericHitbox, to, true, BlockUtils.solid, BlockUtils.semi_solid)) {
                        cancel("type: land", loc, 10, true, 5.0);
                    }
                }
            }
        }
    }

    // Spoof

    private void checkSpoof(SpartanLocation to, SpartanLocation from,
                            double vertical,
                            int airTicks) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)
                && check.getCheck().getBooleanOption("check_spoof", true)
                && (airTicks >= 20 || airTicks >= 12 && player.isFalling(vertical))
                && player.isOnGround() && !player.isOnGroundCustom() && player.isGroundSpoofing()
                && !player.getHandlers().has(Handlers.HandlerType.ElytraWear)
                && !PatternUtils.isBlockPattern(spoof, to, true, BlockUtils.solid, BlockUtils.semi_solid)) {
            cancel("type: spoof, air: " + airTicks + ", vertical: " + vertical, from, 10, true, 0.0);
        }
    }

    // Ratio

    private void manageRatio(SpartanLocation to, SpartanLocation from,
                             double vertical,
                             int airTicks,
                             boolean requiresSuspicion,
                             Buffer buffer, Decimals decimals) {
        if (!player.getHandlers().has(Handlers.HandlerType.BlockBreak)) {
            if (check.getCheck().getBooleanOption("check_ratio", true)
                    && !PlayerData.hasJumpEffect(player)
                    && !BouncingBlocks.isBelow(player)) {
                if (buffer.get(ratioKey + "cancelled") != 1) {
                    int toY = to.getBlockY(), fromY = from.getBlockY();

                    if (toY < fromY) {
                        int blockY = buffer.get(ratioKey + "block-y", Integer.MIN_VALUE);

                        if (blockY != Integer.MIN_VALUE && blockY != toY) {
                            buffer.increase(ratioKey + "blocks", 1);
                        }
                        buffer.set(ratioKey + "block-y", toY);

                        if (vertical < -(MoveUtils.gravityAcceleration * 8) && airTicks > 8) {
                            buffer.set(ratioKey + "validated", 1);
                        }
                    } else if (buffer.get(ratioKey + "validated") == 1 && Math.abs(toY - fromY) <= 3.0 && to.distance(from) <= MoveUtils.chunk) {
                        if (!player.isJumping(vertical)) {
                            int cases, bufferTicks = 5, bufferRequirement = 2;

                            if (player.isOnGroundCustom() && (!player.isBedrockPlayer() || buffer.start(ratioKey + "ground", bufferTicks) >= bufferRequirement)) { // Attention: p.isOnGroundCustom() replace isOnGround(to, 0, 0, 0) recently
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
                                SpartanBukkit.runDelayedTask(player, () -> {
                                    if (player != null) {
                                        if (!PatternUtils.isBlockPattern(ratio, to, true, BlockUtils.semi_solid)) {
                                            int blocks = buffer.get(ratioKey + "blocks");

                                            // bigger or "equal" to 3 due to abstract counting of blocks
                                            if (blocks >= MoveUtils.fallDamageBlocks && !decimals.has(decimals.get(ratioKey + "damage"))) {
                                                boolean safe = blocks >= (MoveUtils.fallDamageBlocks + 1);
                                                cancel("type: ratio, blocks: " + blocks + ", case: " + cases, to, safe ? 10 : 0, true, safe ? blocks : 0.0);
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

    public void manageRatio(EntityDamageEvent.DamageCause cause,
                            double dmg, boolean cancelled) {
        String s = "ratio=";

        if (!check.getCheck().getBooleanOption("check_ratio", true) || PlayerData.hasJumpEffect(player) || cause != EntityDamageEvent.DamageCause.FALL) {
            clearRatio(player.getBuffer(check), player.getDecimals(check));
        } else if (cancelled) {
            Buffer buffer = player.getBuffer(check);
            clearRatio(buffer, player.getDecimals(check));
            buffer.set(s + "cancelled", 1);
        } else {
            player.getDecimals(check).set(s + "damage", dmg);
        }
    }

    private void clearRatio(Buffer buffer, Decimals decimals) {
        buffer.remove(new String[]{ratioKey + "blocks", ratioKey + "block-y", ratioKey + "cancelled", ratioKey + "validated", ratioKey + "movement"});
        decimals.remove(ratioKey + "damage");
    }

    // Implementation

    public NoFall(SpartanPlayer player) {
        super(check, player);
    }

    @Override
    public void handle(Event event) {
        if (event instanceof EntityDamageEvent) {
            player.getCooldowns(check).add("land", 10);
        }
    }

    @Override
    public void run() {
        float fallDistance = player.getFallDistance();
        int airTicks = player.getTicksOnAir();
        double vertical = player.getNmsVerticalDistance();
        double horizontal = player.getNmsHorizontalDistance();
        double oldVertical = player.getOld_NmsVerticalDistance();

        if (fallDistance > 0.0f
                || airTicks > 0
                || horizontal >= 0.1
                || vertical != 0.0
                || oldVertical != 0.0) {
            SpartanLocation to = player.getEventToLocation();
            SpartanLocation from = player.getEventFromLocation();
            Buffer buffer = player.getBuffer(check);
            Decimals decimals = player.getDecimals(check);

            if (canDo()) {
                AsyncDetection.run(player, check, () -> {
                    boolean requiresSuspicion = player.getProfile().isSuspectedOrHacker(check),
                            offGround = player.getBlocksOffGround(3, true, true) >= 2;
                    boolean bouncingBlocks = BouncingBlocks.isBelow(player);

                    if (!bouncingBlocks) {
                        if (requiresSuspicion) {
                            checkLanding(to, from, vertical, fallDistance);
                        }
                        manageRatio(to, from, vertical, airTicks, requiresSuspicion, buffer, decimals);
                    } else {
                        clearRatio(buffer, decimals);
                    }

                    if (offGround && vertical <= -0.1 && !bouncingBlocks) {
                        if (to.getBlockY() < from.getBlockY()) {
                            double oldFallDistance = decimals.get("old");

                            if (decimals.has(oldFallDistance)) {
                                float fallDistanceDifference = player.getFallDistance() - (float) oldFallDistance;
                                checkAir(to, from, fallDistanceDifference, (fallDistance - Math.floor(fallDistance)), vertical, airTicks, requiresSuspicion);

                                if (requiresSuspicion) {
                                    checkPattern(to, from, buffer, fallDistanceDifference, vertical, airTicks);
                                }
                            }
                        }
                        if (!Compatibility.CompatibilityType.MajorIncompatibility.isFunctional()) {
                            checkSpoof(to, from, vertical, airTicks);
                        }
                    }
                });
            }
        }
        player.getDecimals(check).set("old", fallDistance);
    }

    @Override
    public boolean canDo() {
        if (player.canRunCheck(check, true)) {
            GameMode gameMode = player.getGameMode();

            if (gameMode == GameMode.SURVIVAL || gameMode == GameMode.ADVENTURE) {
                EntityDamageEvent damageCause = player.getLastDamageCause();
                SpartanLocation loc = player.getEventToLocation();

                if ((damageCause.getCause() != EntityDamageEvent.DamageCause.FALL || !damageCause.isCancelled())
                        && !player.isInvulnerable()
                        && !player.wasInLiquids()
                        && loc.getBlockY() > BlockUtils.getMinHeight(player.getWorld())
                        && !player.getHandlers().has(Handlers.HandlerType.ElytraUse)
                        && !player.getHandlers().has(Handlers.HandlerType.Vehicle)
                        && !player.getHandlers().has(Handlers.HandlerType.WaterElevator)
                        && !player.getHandlers().has(Handlers.HandlerType.Explosion)
                        && !NoHitDelay.hasCooldown(player)
                        && !Building.hasCooldown(player)
                        && !PlayerData.hasLevitationEffect(player)
                        && !PlayerData.hasSlowFallingEffect(player)
                        && !FloorProtection.hasCooldown(player, true)
                        && !BlockUtils.canClimb(loc)
                        && !player.stepsOnBoats()
                        && (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15) || player.getWorld().getGameRuleValue(GameRule.FALL_DAMAGE) == true)) {
                    ItemStack boots = player.getInventory().getBoots();

                    if (boots != null && boots.containsEnchantment(Enchantment.PROTECTION_FALL)
                            || PatternUtils.isBlockPattern(canDo, loc, true, BlockUtils.web)) {
                        clearRatio(player.getBuffer(check), player.getDecimals(check));
                        return false;
                    }
                    return true;
                }
            }
        }
        clearRatio(player.getBuffer(check), player.getDecimals(check));
        return false;
    }
}
