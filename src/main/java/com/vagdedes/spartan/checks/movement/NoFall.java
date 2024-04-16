package com.vagdedes.spartan.checks.movement;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.data.Cooldowns;
import com.vagdedes.spartan.abstraction.data.Decimals;
import com.vagdedes.spartan.abstraction.data.Handlers;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayerDamage;
import com.vagdedes.spartan.compatibility.manual.damage.NoHitDelay;
import com.vagdedes.spartan.functionality.identifiers.complex.predictable.BouncingBlocks;
import com.vagdedes.spartan.functionality.identifiers.complex.predictable.Damage;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.PatternUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.enchantments.Enchantment;
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

    public NoFall(SpartanPlayer player) {
        super(check, player);
    }

    private void checkAir(SpartanLocation to, SpartanLocation from,
                          float fallDistanceDifference,
                          double fallDistanceRemaining, double vertical,
                          int airTicks,
                          boolean requiresSuspicion) {
        if (hackType.getCheck().getBooleanOption("check_air", true)) {
            String cooldownKey = "air=cooldown";
            int cooldownTicks = 10;

            if (airTicks > 0 && vertical > -3.5 && !player.getHandlers().has(Handlers.HandlerType.ElytraWear) && !PlayerUtils.hasJumpEffect(player)) {
                if (PatternUtils.isBlockPattern(air, to, true, BlockUtils.semi_solid)) {
                    player.getCooldowns(hackType).add(cooldownKey, cooldownTicks);
                    return;
                }
                if (!player.getCooldowns(hackType).canDo(cooldownKey)) {
                    return;
                }

                if (fallDistanceDifference <= 0.0f
                        && fallDistanceRemaining == 0.0
                        && (requiresSuspicion || airTicks >= 8 || vertical <= -0.5)) {
                    int differenceInFall = Math.abs(PlayerUtils.getFallingTick(vertical) - player.movement.getTicksOnAir());

                    if (differenceInFall <= 7) {
                        cancel("type: air, fall-distance: " + fallDistanceDifference + ", air: " + airTicks + ", distance: " + vertical + ", difference: " + differenceInFall, from, 5, true, 2.0);
                    }
                }
            } else {
                player.getCooldowns(hackType).add(cooldownKey, cooldownTicks);
            }
        }
    }

    private void checkPattern(SpartanLocation to, SpartanLocation from,
                              Buffer buffer,
                              Decimals decimals,
                              float fallDistanceDifference,
                              double vertical,
                              int airTicks) {
        if (hackType.getCheck().getBooleanOption("check_pattern", true)
                && !player.isOnGroundCustom()
                && !PlayerUtils.isOnGround(player, to, 0, -1, 0)
                && !PlayerUtils.hasJumpEffect(player)) {
            String key = "pattern";

            if (fallDistanceDifference > 0.0f) {
                buffer.increase(key, 1);
            } else {
                int pattern = buffer.get(key);

                if (pattern > 1) {
                    double cachedPattern = decimals.get(key + "=cache", Double.MIN_VALUE);

                    if (cachedPattern != Double.MIN_VALUE
                            && cachedPattern == pattern) {
                        cancel("type: pattern, fall-distance-difference: " + fallDistanceDifference + ", air: " + airTicks + ", distance: " + vertical, from, 5, true, 2.0);
                    }
                    decimals.set(key + "=cache", pattern);
                }
                buffer.remove(key);
            }
        }
    }

    private void checkLanding(SpartanLocation to, SpartanLocation loc, double vertical, float fall) {
        if (hackType.getCheck().getBooleanOption("check_landing", true)
                && !PlayerUtils.hasSpeedEffect(player, to, true) && !PlayerUtils.hasJumpEffect(player)) {
            String key = "land";

            if (vertical < 0.0) {
                if (fall >= 6.0f) {
                    player.getCooldowns(hackType).add(key, 10);
                }
            } else {
                Cooldowns cooldowns = player.getCooldowns(hackType);

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

    private void checkSpoof(SpartanLocation to, SpartanLocation from,
                            double vertical,
                            int airTicks) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)
                && hackType.getCheck().getBooleanOption("check_spoof", true)
                && (airTicks >= 20 || airTicks >= 12 && player.movement.isFalling(vertical))
                && player.isOnGround()
                && !player.isOnGroundCustom()
                && player.isGroundSpoofing()
                && !player.getHandlers().has(Handlers.HandlerType.ElytraWear)
                && !PatternUtils.isBlockPattern(spoof, to, true, BlockUtils.solid, BlockUtils.semi_solid)) {
            cancel("type: spoof, air: " + airTicks + ", vertical: " + vertical, from, 10, true, 0.0);
        }
    }

    // Separator

    private void manageRatio(SpartanLocation to, SpartanLocation from,
                             double vertical,
                             int airTicks,
                             boolean requiresSuspicion,
                             Buffer buffer, Decimals decimals) {
        if (!player.getHandlers().has(Handlers.HandlerType.SensitiveBlockBreak)) {
            if (hackType.getCheck().getBooleanOption("check_ratio", true)
                    && !PlayerUtils.hasJumpEffect(player)
                    && !BouncingBlocks.isBelow(player)
                    && buffer.get(ratioKey + "cancelled") != 1
                    && player.getCooldowns(check).canDo("ratio")) {
                int toY = to.getBlockY(), fromY = from.getBlockY();

                if (toY < fromY) {
                    double blockY = decimals.get(ratioKey + "block-y", Double.MIN_VALUE);

                    if (blockY != Double.MIN_VALUE && blockY != toY) {
                        buffer.increase(ratioKey + "blocks", 1);
                    }
                    decimals.set(ratioKey + "block-y", toY);

                    if (vertical < -(PlayerUtils.gravityAcceleration * 8) && airTicks > 8) {
                        buffer.set(ratioKey + "validated", 1);
                    }
                } else if (buffer.get(ratioKey + "validated") == 1
                        && Math.abs(toY - fromY) <= 3.0
                        && to.distance(from) <= PlayerUtils.chunk) {
                    if (!player.movement.isJumping(vertical)) {
                        int cases, bufferTicks = 5, bufferRequirement = 2;

                        if (player.isOnGroundCustom()
                                && (!player.bedrockPlayer
                                || buffer.start(ratioKey + "ground", bufferTicks, Check.detectionMeasurementTicks) >= (bufferRequirement / (double) bufferTicks))) { // Attention: p.isOnGroundCustom() replace isOnGround(to, 0, 0, 0) recently
                            cases = 1;
                        } else if (vertical == 0.0
                                && (requiresSuspicion
                                || buffer.start(ratioKey + "movement", bufferTicks, Check.detectionMeasurementTicks) >= (bufferRequirement / (double) bufferTicks))) {
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
                                        if (blocks >= PlayerUtils.fallDamageBlocks && !decimals.has(decimals.get(ratioKey + "damage"))) {
                                            boolean safe = blocks >= (PlayerUtils.fallDamageBlocks + 1);
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
        }
    }

    public void manageRatio(EntityDamageEvent.DamageCause cause,
                            double dmg, boolean cancelled) {
        String s = "ratio=";

        if (!hackType.getCheck().getBooleanOption("check_ratio", true) || PlayerUtils.hasJumpEffect(player) || cause != EntityDamageEvent.DamageCause.FALL) {
            clearRatio(player.getBuffer(hackType), player.getDecimals(hackType));
        } else if (cancelled) {
            Buffer buffer = player.getBuffer(hackType);
            clearRatio(buffer, player.getDecimals(hackType));
            buffer.set(s + "cancelled", 1);
        } else {
            player.getDecimals(hackType).set(s + "damage", dmg);
        }
    }

    private void clearRatio(Buffer buffer, Decimals decimals) {
        buffer.remove(new String[]{ratioKey + "blocks", ratioKey + "block-y", ratioKey + "cancelled", ratioKey + "validated", ratioKey + "movement"});
        decimals.remove(ratioKey + "damage");
    }

    // Separator

    @Override
    public boolean handleInternal(boolean cancelled, Object object) {
        player.getCooldowns(hackType).add(new String[]{"land", "ratio"}, 10);
        return false;
    }

    @Override
    public boolean runInternal(boolean cancelled) {
        Double vertical = player.movement.getNmsVerticalDistance(),
                horizontal = player.movement.getNmsHorizontalDistance(),
                oldVertical = player.movement.getPreviousNmsVerticalDistance();

        if (vertical == null
                || horizontal == null
                || oldVertical == null) {
            return false;
        }
        float fallDistance = player.getFallDistance();
        int airTicks = player.movement.getTicksOnAir();

        if (fallDistance > 0.0f
                || airTicks > 0
                || horizontal >= 0.1
                || vertical != 0.0
                || oldVertical != 0.0) {
            SpartanLocation to = player.movement.getEventToLocation();
            SpartanLocation from = player.movement.getEventFromLocation();
            Buffer buffer = player.getBuffer(hackType);
            Decimals decimals = player.getDecimals(hackType);

            if (canDo()) {
                runAsync(() -> {
                    boolean requiresSuspicion = player.getProfile().isSuspectedOrHacker(hackType),
                            offGround = player.getBlocksOffGround(4) >= 3;
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
                                    checkPattern(to, from, buffer, decimals, fallDistanceDifference, vertical, airTicks);
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
        player.getDecimals(hackType).set("old", fallDistance);
        return false;
    }

    @Override
    protected boolean canDo() {
        if (player.canRunCheck(hackType, true)
                && !player.movement.isCrawling()) {
            GameMode gameMode = player.getGameMode();

            if (gameMode == GameMode.SURVIVAL || gameMode == GameMode.ADVENTURE) {
                SpartanPlayerDamage damage = player.getLastDamageReceived();
                SpartanLocation loc = player.movement.getEventToLocation();

                if ((damage == null
                        || damage.ticksPassed() > TPS.maximum
                        || damage.event.getCause() != EntityDamageEvent.DamageCause.FALL)
                        && !player.isInvulnerable()
                        && !player.movement.wasInLiquids()
                        && loc.getBlockY() > BlockUtils.getMinHeight(player.getWorld())
                        && !player.getHandlers().has(Handlers.HandlerType.ElytraUse)
                        && !player.getHandlers().has(Handlers.HandlerType.Vehicle)
                        && !player.getHandlers().has(Handlers.HandlerType.WaterElevator)
                        && !Damage.hasExplosion(player)
                        && !player.getHandlers().has(Handlers.HandlerType.TowerBuilding)
                        && !NoHitDelay.hasCooldown(player)
                        && !PlayerUtils.hasLevitationEffect(player)
                        && !PlayerUtils.hasSlowFallingEffect(player)
                        && !player.getLastDamageReceived().isSubtle()
                        && !BlockUtils.canClimb(loc)
                        && !player.movement.stepsOnBoats()
                        && (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15) || player.getWorld().getGameRuleValue(GameRule.FALL_DAMAGE) == true)) {
                    ItemStack boots = player.getInventory().getBoots();

                    if (boots != null && boots.containsEnchantment(Enchantment.PROTECTION_FALL)
                            || PatternUtils.isBlockPattern(canDo, loc, true, BlockUtils.web)) {
                        clearRatio(player.getBuffer(hackType), player.getDecimals(hackType));
                        return false;
                    }
                    return true;
                }
            }
        }
        clearRatio(player.getBuffer(hackType), player.getDecimals(hackType));
        return false;
    }
}
