package me.vagdedes.spartan.checks.movement.speed;

import me.vagdedes.spartan.compatibility.manual.abilities.crackshot.CrackShot;
import me.vagdedes.spartan.compatibility.manual.damage.NoHitDelay;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.features.performance.AsyncDetection;
import me.vagdedes.spartan.features.protections.Building;
import me.vagdedes.spartan.features.synchronicity.SpartanEdition;
import me.vagdedes.spartan.handlers.identifiers.BouncingBlocks;
import me.vagdedes.spartan.handlers.identifiers.Damage;
import me.vagdedes.spartan.handlers.identifiers.FishingHook;
import me.vagdedes.spartan.handlers.identifiers.FloorProtection;
import me.vagdedes.spartan.handlers.tracking.MovementProcessing;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.data.Cooldowns;
import me.vagdedes.spartan.objects.data.Decimals;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.hackPrevention.HackPrevention;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.gameplay.*;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Speed {

    public static final HackType check = HackType.Speed;
    public static final String strongPrevention = "normal";

    private static final double walking = 0.292;
    public static double ground = 0.325;
    private static final double incompatibility = 0.75;
    private static final double ground_block_speed = 0.46;
    private static final double ground_block_speed_extended = 0.57;
    private static final double ground_slime = 0.31;
    private static final double air_slime = 0.47;
    private static final double ground_jump_speed = 0.525;
    private static double air = 0.3675;
    private static final double air_decreased = 0.366;
    private static final double air_non_solid_min = 0.42;
    private static final double air_non_solid_max = 1.0;
    private static final double blocks = 0.7;
    private static final double blocks_slime = 0.85;
    private static final double blocks_ice = 1.55;
    public static final double ypos = 0.68;
    private static final double ypos_advanced = 0.71;
    private static final double upwards = 0.78;
    private static final double sneaking = 0.16;
    private static final double cobweb = 0.1;
    private static final double interactiveBush = 0.15;
    static final double ice = 0.69; // 0.59 previously
    private static final double selfHit = 1.6;
    private static final double soul_sand_ground = 0.2;
    private static final double solid = 1.5;
    private static final double bed = 0.62;
    private static final double dmg = 2.15;
    private static final double trident = 4.0;
    private static final double bow = 3.0;
    static final double piston = 1.6;
    private static final double pushExtreme = 6.0;
    private static final double pushFair = (pushExtreme / 4.0);
    private static final Set<Float> fallingDistance = new HashSet<>(6);

    private static final double[][]
            honey = new double[][]{
            {BlockUtils.hitbox_max, -2, BlockUtils.hitbox_max},
            {BlockUtils.hitbox_max, -1, BlockUtils.hitbox_max},
            {BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max}
    }, groundLocations = new double[][]{
            {1, -1, 1},
            {1, 0, 1},
            {1, 1, 1}
    }, fallDistance = new double[][]{
            {1, -1, 1},
            {1, 0, 1},
            {1, 1, 1}
    }, hopIllegal = new double[][]{
            {1, -1, 1},
            {1, 0, 1},
            {1, 1, 1}
    }, upwardsLocations = new double[][]{
            {BlockUtils.hitbox, -1, BlockUtils.hitbox},
            {BlockUtils.hitbox, 0, BlockUtils.hitbox}
    };
    private static final double[]
            minus_0_6 = new double[]{
            BlockUtils.hitbox_max, -0.6, BlockUtils.hitbox_max
    }, minusOne = new double[]{
            0, -1, 0
    }, genericHitboxMax = new double[]{
            BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max
    }, genericHitbox = new double[]{
            BlockUtils.hitbox, 0, BlockUtils.hitbox
    }, aboveBlock = new double[]{
            BlockUtils.hitbox, 2, BlockUtils.hitbox
    }, hopIllegalSingle = new double[]{
            1, 2, 1
    };

    static {
        float f = (float) MoveUtils.falling[0];
        fallingDistance.add(0.0f);
        fallingDistance.add(f);
        fallingDistance.add(f * 3);
        fallingDistance.add(f * 6);
        fallingDistance.add(f * 10);
        fallingDistance.add(f * 14.5f);

        /*
        fallingDistance.add(0.0784f);
        fallingDistance.add(0.075444065f);

        fallingDistance.add(0.22777925f);
        fallingDistance.add(0.233632f);

        fallingDistance.add(0.45546773f);
        fallingDistance.add(0.46415937f);

        fallingDistance.add(0.7570025f);
        fallingDistance.add(0.7684762f);

        fallingDistance.add(1.1309065f);
        fallingDistance.add(1.1451067f);
        */
    }

    public static void remove(SpartanPlayer p) {
        SprintSpeed.remove(p);
    }

    public static void updateLimits() {
        air = Compatibility.CompatibilityType.ViaRewind.isFunctional() ? 0.39 : air;
    }

    public static void run(SpartanPlayer p, SpartanLocation to, SpartanLocation from, double ver, double hor, int air, double box, double oldVer, double oldHor, double oldBox, boolean groundGliding) {
        if (hor < sneaking || p.wasInLiquids() || !canDo(p, to)) {
            return;
        }
        Cooldowns cooldowns = p.getCooldowns(check);
        Decimals decimals = p.getDecimals(check);
        double dm = getGreaterRepetitiveLimit(decimals);
        String reason;
        boolean aboveBlocks = false;
        boolean groundVanilla = p.isOnGround();
        boolean groundCustom = p.isOnGroundCustom();
        boolean jumping = p.isJumping(ver);
        boolean extremelyPushed = p.getHandlers().has(Handlers.HandlerType.ExtremeCollision);
        boolean fairlyPushed = extremelyPushed || PlayerData.hasNearbyCollisions(p);
        boolean piston = p.getHandlers().has(Handlers.HandlerType.Piston);
        SpartanLocation loc1 = to.clone().add(0, 1, 0);
        SpartanLocation locM1 = to.clone().add(0, -1, 0);
        boolean honey = !PatternUtils.isBlockPattern(Speed.honey, to, true, BlockUtils.honey_block);

        if (groundGliding
                || isSolid(to, box)
                && (PlayerData.isOnGround(p, to, 0, -box, 0)
                || PlayerData.isOnGround(p, to, 0, -(box * 2), 0))) {
            reason = "blocks";
            aboveBlocks = true;

            if (piston) {
                dm = cacheLimit(decimals, dm, blocks_ice, 15);
            } else if (p.getHandlers().has(Handlers.HandlerType.BlockBreak) || BlockUtils.isSlime(p, to, 2)) {
                dm = cacheLimit(decimals, dm, blocks_slime, 15);
            } else {
                dm = cacheLimit(decimals, dm, blocks, 15);
            }
        } else if (BlockUtils.isFullSolid(loc1)) {
            reason = "solid";
            dm = cacheLimit(decimals, dm, solid, 10);
        } else if (oldVer == 0.0 && ver == 0.0 || (ver - oldVer) == 0.0 && (!groundVanilla || !groundCustom)) {
            Material material;

            if (PatternUtils.isBlockPattern(new double[][]{
                    {BlockUtils.hitbox, 0, BlockUtils.hitbox},
                    {BlockUtils.hitbox, 1, BlockUtils.hitbox},
                    {BlockUtils.hitbox, p.getEyeHeight() + MoveUtils.lowPrecision, BlockUtils.hitbox},
            }, to, true, BlockUtils.web)) {
                reason = "cobweb";
                dm = cacheLimit(decimals, dm, cobweb, 5);
            } else if (PatternUtils.isBlockPattern(new double[][]{
                    {BlockUtils.hitbox, 0, BlockUtils.hitbox},
                    {BlockUtils.hitbox, 1, BlockUtils.hitbox},
                    {BlockUtils.hitbox, p.getEyeHeight() + MoveUtils.lowPrecision, BlockUtils.hitbox},
            }, to, true, BlockUtils.interactive_bushes)) {
                reason = "interactive-bush";
                dm = cacheLimit(decimals, dm, interactiveBush, 5);
            } else if (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15) && p.isSneaking()) {
                reason = "sneaking";
                dm = cacheLimit(decimals, dm, sneaking, 0);
            } else if ((material = to.getBlock().getType()) == Material.SOUL_SAND || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16) && material == Material.SOUL_SOIL) {
                reason = "soul-sand";

                if (PlayerData.hasSoulSpeedEnchantment(p, to)) {
                    dm = cacheLimit(decimals, dm, ground, 5);
                } else {
                    dm = cacheLimit(decimals, dm, soul_sand_ground, 5);
                }
            } else {
                material = locM1.getBlock().getType();

                if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8) && material == Material.SLIME_BLOCK || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15) && material == Material.HONEY_BLOCK) {
                    dm = cacheLimit(decimals, dm, ground_slime, 0);
                } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15) || p.isSprinting()) {
                    dm = cacheLimit(decimals, dm, ground, 0);
                } else {
                    dm = cacheLimit(decimals, dm, walking, 0);
                }

                if (dm != walking) {
                    reason = "ground";
                    dm = cacheLimit(decimals, dm, dm, 15);
                } else {
                    reason = "walking";
                    dm = cacheLimit(decimals, dm, dm, 10);
                }
            }
        } else if (Math.abs(MoveUtils.jumping[1] - ver) < MoveUtils.jumping[2]
                || Math.abs(MoveUtils.jumping[1] - box) < MoveUtils.jumping[2]) {
            reason = "ypos";
            if (!cooldowns.canDo("ice=reminder")) {
                dm = cacheLimit(decimals, dm, ypos_advanced, 0);
            } else {
                dm = cacheLimit(decimals, dm, ypos, 0);
            }
        } else if ((box >= 0.084 && box <= 0.15 || box >= 0.75 || GroundUtils.heightExists(box, true))
                && ver == 0.0 && !BlockUtils.isGroundFree(p, to, 0.2, 0, 0.2) && (groundVanilla || groundCustom)) {
            reason = "ground";

            if (hor == oldHor) {
                dm = cacheLimit(decimals, dm, ground_block_speed_extended, 5);
            } else {
                dm = cacheLimit(decimals, dm, ground_block_speed, 5);
            }
        } else {
            reason = "air";

            if (getGreaterRepetitiveLimit(decimals) == blocks_ice || !cooldowns.canDo("ice=reminder")) {
                dm = cacheLimit(decimals, dm, ice, 10);
            } else if (honey || BlockUtils.isSlime(p, to, 3)) {

                if (PatternUtils.isBlockPattern(aboveBlock, to, true, BlockUtils.solid)) {
                    dm = cacheLimit(decimals, dm, blocks_slime, 12);
                } else {
                    dm = cacheLimit(decimals, dm, air_slime, 12);
                }
            } else if (PatternUtils.isBlockPattern(genericHitboxMax, to, true, BlockUtils.carpet)
                    || to.isBlock(Material.SNOW, BlockUtils.hitbox_max)) {
                if (!PatternUtils.isBlockPattern(minus_0_6, to, true, BlockUtils.walls, BlockUtils.anvil)) {
                    dm = cacheLimit(decimals, dm, air_non_solid_min, 8);
                } else {
                    dm = cacheLimit(decimals, dm, air_non_solid_max, 8);
                }
            } else {
                if (!jumping && hor >= 0.33 && box > 0.0 && box < 0.1 && PlayerData.isOnGround(p, to, 0, -1, 0)) {
                    cooldowns.add("skip-ground-jump-limit", 10);
                }
                if (FloorProtection.hasCooldown(p)) {
                    dm = cacheLimit(decimals, dm, air_non_solid_min, 8);
                } else {
                    dm = cacheLimit(decimals, dm, Speed.air, 8);
                }
            }
        }

        // Limit
        boolean selfHit = Damage.hasCooldown(p, Damage.selfHitKey);
        Buffer buffer = p.getBuffer(check);
        Object[] dynamicLimit = getDynamicLimit(p, to, loc1, buffer, decimals, cooldowns,
                hor, oldHor, ver, box, oldBox, air,
                jumping, extremelyPushed, fairlyPushed, aboveBlocks, groundCustom, selfHit, piston,
                dm, reason);
        dm = (double) dynamicLimit[0];
        reason = (String) dynamicLimit[1];

        // Always after calculating all limits
        double limitTicks = getLimitTicks(decimals);

        if (decimals.has(limitTicks)) {
            limitTicks -= 1;

            if (limitTicks <= 0) {
                decimals.remove(new String[]{
                        "repetitive-limit",
                        "ticks"
                });
            } else {
                decimals.set("ticks", limitTicks);
            }
        }
        String reasonCopy = reason;
        boolean bedrockPlayer = p.isBedrockPlayer(),
                hasTrident = p.getHandlers().has(Handlers.HandlerType.Trident),
                explosion = !hasTrident && p.getHandlers().has(Handlers.HandlerType.Explosion),
                damage = Damage.hasCooldown(p),
                knockback = Damage.hasCooldown(p, Damage.knockbackKey),
                hitDelay = damage && NoHitDelay.hasCooldown(p),
                incompatibilities = Compatibility.CompatibilityType.MajorIncompatibility.isFunctional();
        float difference = PlayerData.getWalkSpeedDifference(p);
        double minimumLimit = Math.max(
                bedrockPlayer || incompatibilities ? (bedrockPlayer && SpartanEdition.freeAlternativeVersion ? incompatibility * 2.0 : incompatibility) :
                        p.getHandlers().has(Handlers.HandlerType.ElytraWear)
                                || Compatibility.CompatibilityType.MythicMobs.isFunctional()
                                || Compatibility.CompatibilityType.mcMMO.isFunctional() ? 0.6 :
                                0.0,
                check.getCheck().getDecimalOption("minimum_limit_in_blocks", 0.0)
        ), modifiedLimit = adjustLimit(p, to, reason, dm, difference, hasTrident, knockback, hitDelay, explosion, selfHit, damage, minimumLimit),
                dmCopy = dm;
        SprintSpeed.setLimit(p, modifiedLimit);

        if (p.isSprinting() && (modifiedLimit == ground || modifiedLimit == Speed.air)) {
            decimals.set("average=sprinting=speed", hor);
        }

        if (dm != blocks && dm != blocks_slime && (reason.length() != 6 || !reason.equals("blocks"))
                && !incompatibilities && !bedrockPlayer && difference == 0.0f && !damage && !extremelyPushed && !hasTrident && !hitDelay && minimumLimit <= 0.0
                && !PlayerData.hasSpeedEffect(p, to) && !PlayerData.hasJumpEffect(p) && !FishingHook.hasCooldown(p) && !piston
                && !PlayerData.hasLevitationEffect(p) && !FloorProtection.hasCooldown(p) && !p.getHandlers().has(Handlers.HandlerType.WaterElevator)) {
            boolean aboveBlocksCopy = aboveBlocks;

            AsyncDetection.run(p, check, () -> {
                boolean building = Building.hasCooldown(p);

                // Separator
                if (modifiedLimit == Speed.air && !honey && !explosion && hor >= air_decreased
                        && buffer.start("value-decrease", 50) == 3) {
                    buffer.remove("value-decrease");
                    cancel(p, from, "type: air(decreased), horizontal: " + hor);
                }

                //Separator
                if (ver > 0.0 && air <= 12 && !explosion
                        && (dmCopy == Speed.air || dmCopy == ground || dmCopy == ground_jump_speed || dmCopy == ground_block_speed
                        || dmCopy == ground_block_speed_extended || dmCopy == sneaking || dmCopy == walking)
                        && (!groundVanilla || !groundCustom)
                        && !BouncingBlocks.isBelow(p)
                        && !BlockUtils.isSlime(p, to, 2)
                        && !PatternUtils.isBlockPattern(hopIllegalSingle, to, true, BlockUtils.solid)
                        && !PatternUtils.isBlockPattern(hopIllegal, to, true, BlockUtils.semi_solid)) {
                    if (!jumping) {
                        if (!BlockUtils.isSlime(p, to, 3) && buffer.start("hop=illegal", 10) >= 4) {
                            cancel(p, from, "type: hop(illegal), horizontal: " + hor + ", limit: " + dmCopy);
                        }
                    } else if (cooldowns.canDo("above-block")) {
                        boolean cancel = false;

                        if (Math.abs(MoveUtils.jumping[0] - ver) < MoveUtils.jumping[2] && hor >= 0.35) {
                            cancel = true;
                        } else if (Math.abs((MoveUtils.jumping[0] * 2) - ver) < MoveUtils.jumping[2] && hor >= 0.355) {
                            cancel = true;
                        } else if (Math.abs((MoveUtils.jumping[0] * 3) - ver) < MoveUtils.jumping[2] && hor >= 0.36) {
                            cancel = true;
                        } else if (Math.abs((MoveUtils.jumping[0] * 4) - ver) < MoveUtils.jumping[2] && hor >= 0.365) {
                            cancel = true;
                        }

                        if (cancel) {
                            cancel(p, from, "type: hop(acceleration), vertical: " + ver + ", horizontal: " + hor + ", limit: " + dmCopy);
                        }
                    }
                }

                //Separator
                if (dmCopy == upwards) {
                    double diff = Math.abs(hor - oldHor);

                    if (hor > oldHor && diff >= 0.35 && ver == 0.5) {
                        cancel(p, from, "type: upwards(difference), horizontal: " + hor + ", old-horizontal: " + oldHor + ", difference: " + diff);
                    }
                }

                // Separator
                if (oldHor != 0.0) {
                    double diff = Math.abs(hor - oldHor);
                    int iceTicks = buffer.get("ice=ticks");

                    if (((boolean) dynamicLimit[2]) && (dmCopy == ice || dmCopy == ypos_advanced)) {
                        iceTicks += 1;
                        buffer.set("ice=ticks", iceTicks);
                    } else {
                        iceTicks = 0;
                        buffer.remove("ice=ticks");
                    }

                    if (hor > oldHor) {
                        if (aboveBlocksCopy && dmCopy == blocks_ice && diff >= 0.33) {
                            cancel(p, from, "type: ice-speed(blocks), difference: " + diff + ", horizontal: " + hor + ", old-horizontal: " + oldHor);
                        } else if (iceTicks >= 20 && hor >= 0.5 && (diff >= 0.25 || diff <= 0.15)) {
                            cancel(p, from, "type: ice-speed(normal), difference: " + diff + ", horizontal: " + hor + ", old-horizontal: " + oldHor);
                        }
                    }
                }

                // Separator
                if (dmCopy == ice && !aboveBlocksCopy && !piston) {
                    if (hor >= 0.5 && buffer.start("ice_speed=counter", 20) >= 7) {
                        cancel(p, from, "type: ice-speed(counter), horizontal: " + hor + ", limit: " + dmCopy);
                    }
                    if (groundCustom && hor >= (dmCopy * (AlgebraUtils.oneThird * 2.0))
                            && buffer.canDo(reasonCopy)
                            && !PatternUtils.isBlockPattern(genericHitbox, to, true, BlockUtils.semi_solid)
                            && PatternUtils.isBlockPattern(minusOne, to, true, BlockUtils.ice)
                            && buffer.start("ice_speed", 10) == 4) {
                        buffer.remove("ice_speed");
                        cancel(p, from, "type: ice-speed(ground), horizontal: " + hor + ", limit: " + dmCopy);
                    }
                }

                // Separator
                if (!aboveBlocksCopy
                        && hor < modifiedLimit
                        && (dmCopy == Speed.air || dmCopy == walking || dmCopy == ground || dmCopy == sneaking || dmCopy == ground_jump_speed)
                        && cooldowns.canDo("above-block")
                        && cooldowns.canDo("bed")
                        && !BlockUtils.isSlime(p, to, 2)) {
                    float fallDistance = p.getFallDistance();

                    if (fallDistance <= 1.2f
                            && (dmCopy != Speed.air && dmCopy != ground_jump_speed
                            || !PatternUtils.isBlockPattern(Speed.fallDistance, to, true, BlockUtils.semi_solid))) {
                        boolean illegal = true;

                        for (float value : fallingDistance) {
                            if (Math.abs(value - fallDistance) < 0.1) {
                                illegal = false;
                                break;
                            }
                        }

                        if (hor >= 0.215 && illegal && !fairlyPushed && air <= 12 && !p.isOnFire() && !BouncingBlocks.isBelow(p) && !building) {
                            cancel(p, from, "type: fall-distance(illegal), horizontal: " + hor + ", limit: " + dmCopy + ", fall-distance: " + fallDistance + ", a: " + air + ", box: " + box + ", vertical: " + ver);
                        } else if (hor > 0.335 && fallDistance > 0.0f && (!groundVanilla || !groundCustom) && !BlockUtils.isSemiSolid(locM1)) {
                            cancel(p, from, "type: fall-distance(normal), horizontal: " + hor + ", limit: " + dmCopy + ", box: " + box + ", vertical: " + ver);

                        } else if (groundCustom && hor >= 0.36 && fallDistance == 0.0f && GroundUtils.isZeroMiscalculation(box)) {
                            if (buffer.start("fall-distance", 10) == 2) {
                                buffer.remove("fall-distance");
                                cancel(p, from, "type: fall-distance(ground), horizontal: " + hor + ", limit: " + dmCopy + ", box: " + box + ", vertical: " + ver);
                            }
                        }
                    }
                }

                // Separator
                if (dmCopy == Speed.air && hor >= 0.34 && hor < modifiedLimit && (!groundVanilla || !groundCustom)
                        && buffer.start("hop=motion", 20) == 12) {
                    buffer.remove("hop=motion");
                    cancel(p, from, "type: hop(motion), horizontal: " + hor + ", limit: " + dmCopy);
                }

                // Separator
                if (dmCopy == ground_jump_speed && hor >= 0.36 && hor < modifiedLimit && ver <= 0.0 && !aboveBlocksCopy
                        && buffer.start("ground_jump_speed", 10) == 3) {
                    buffer.remove("ground_jump_speed");
                    cancel(p, from, "type: ground(jump-speed), horizontal: " + hor + ", limit: " + dmCopy);
                }

                // Separator
                if (hor >= 0.295 && hor < modifiedLimit && !aboveBlocksCopy && !explosion && !building) {
                    boolean allow = false;
                    double v = AlgebraUtils.cut(hor, 8);

                    if (dmCopy == Speed.air && (!groundVanilla || !groundCustom) && PlayerData.isOnGround(p, to, 0, -0.25, 0)) {
                        allow = true;
                    } else if (dmCopy == ground || dmCopy == walking || dmCopy == sneaking) {
                        allow = true;
                    }

                    if (allow
                            && !PatternUtils.isBlockPattern(groundLocations, to, true, BlockUtils.semi_solid)
                            && buffer.start("high_normal_speed", 20) >= 18) {
                        cancel(p, from, "type: ground(high), horizontal: " + hor + ", limit: " + dmCopy);
                    }
                    long time = p.getTimer(check).get("repeated=" + v);

                    if (time <= 250L && buffer.start("repeated", 10) == 3) {
                        buffer.remove("repeated");
                        cancel(p, from, "type: repeated, t: " + time + ", horizontal: " + v + ", limit: " + dmCopy);
                    }
                    p.getTimer(check).set("repeated=" + v);
                }

                // Separator
                if (dmCopy == ypos && reasonCopy.equals("ypos") && hor >= 0.4 && hor < modifiedLimit
                        && buffer.start("ypos=attempts", 20) == 4) {
                    buffer.remove("ypos=attempts");
                    cancel(p, from, "type: ypos, horizontal: " + hor + ", limit: " + dmCopy + ", dynamic-limit: " + modifiedLimit);
                }
            });
        }

        // Detection
        if (modifiedLimit > 0.0 && hor >= modifiedLimit) {
            AsyncDetection.run(p, check, () -> {
                boolean allow = true;

                // Separator
                if (groundCustom || PlayerData.isOnGround(p, to, 0, -1, 0)) {
                    if (modifiedLimit == ground && hor < 0.36
                            || (modifiedLimit == Speed.air || modifiedLimit == ground_block_speed || modifiedLimit == ground_block_speed_extended) && hor < 0.63
                            || modifiedLimit == sneaking && hor < 0.2) {
                        long m = p.getTimer(check).get("limit=protection");

                        if (p.getTimer(check).has(m)) {
                            if (m >= 4000L) {
                                allow = false;
                            }
                        } else {
                            allow = false;
                        }
                    }
                    p.getTimer(check).set("limit=protection");
                }

                if (allow) {
                    int time, attempts;

                    if (hitDelay
                            || explosion
                            || damage && (Damage.getLastReceived(p) <= 500L || Damage.hasCooldown(p, Damage.knockbackKey))) {
                        time = 10;
                        attempts = 6;
                    } else if (PlayerData.hasHighJumpEffect(p)) {
                        time = 5;
                        attempts = 3;
                    } else {
                        time = 1;
                        attempts = 1;
                    }

                    // Separator
                    if (!buffer.canDo(reasonCopy)) {
                        buffer.remove(reasonCopy);
                    }
                    if (buffer.start(reasonCopy, time) == attempts
                            || hasTrident && modifiedLimit == trident
                            || knockback && modifiedLimit == bow
                            || !knockback && (damage && !hasTrident || explosion) && hor >= dmg) {
                        buffer.remove(reasonCopy);
                        cancel(p, from, "type: " + strongPrevention + ", reason: " + reasonCopy + ", horizontal: " +
                                hor + ", limit: " + dmCopy + ", dynamic-limit: " + modifiedLimit + ", vertical: " + ver + ", air: " + air
                                + ", difference: " + Math.abs(hor - oldHor));
                    }
                }
            });
        }
    }

    private static void cancel(SpartanPlayer p, SpartanLocation loc, String message) {
        new HackPrevention(p, check, message, loc, 0, true);
    }

    private static Object[] getDynamicLimit(SpartanPlayer p, SpartanLocation to, SpartanLocation loc1, Buffer buffer, Decimals decimals, Cooldowns cooldowns,
                                            double d, double od, double dy, double remaining, double oremaining, int air_ticks,
                                            boolean jumping, boolean extremelyPushed, boolean fairlyPushed, boolean above_blocks, boolean groundCustom, boolean selfHit, boolean piston,
                                            double dm, String reason) {
        boolean iceBlocks = false;

        // Limit
        for (int i = -1; i <= 0; i++) {
            if (BlockUtils.areIceBlocks(to.clone().add(0, i, 0))) {
                cooldowns.add("ice=reminder", 20);
                int iceBlockTimes = 15;

                if (!above_blocks) {
                    iceBlocks = true;
                    dm = cacheLimit(decimals, dm, ice, 0);

                    if (dm == ice) {
                        reason = "ice";
                        dm = cacheLimit(decimals, dm, dm, iceBlockTimes);
                    }
                } else {
                    dm = cacheLimit(decimals, dm, blocks_ice, iceBlockTimes * 2);
                }
                break;
            }
        }

        // Limit
        if ((p.isSprinting() || p.isSprintJumping())
                && (jumping || dy == 0.375 || dy >= 0.5 && dy < 0.6 || Math.abs(remaining - oremaining) == 0.5)

                && (groundCustom
                || PlayerData.isOnGround(p, to, 0, -1, 0)
                || PlayerData.isOnGround(p, to, 0, -1.5, 0))) {
            // Combined Step Blocks (Not using hitbox_max due to ascension imperfections)
            if ((dm == air || dm == air_non_solid_min || dm == air_non_solid_max) && air_ticks == 0 && (dy > 0.5 || jumping)
                    && PatternUtils.isBlockPattern(upwardsLocations, to, true, BlockUtils.semi_solid)) {
                reason = "upwards(combined)";
                dm = cacheLimit(decimals, dm, upwards, 10);
            } else if (!jumping) {
                SpartanLocation custom = to.clone();
                custom.setPitch(0.0f);
                custom = custom.add(to.getDirection().multiply(1.0));
                boolean nonSolidFree = false;

                for (SpartanLocation customs : custom.getSurroundingLocations(BlockUtils.hitbox, 0, BlockUtils.hitbox)) {
                    if (BlockUtils.isFullSolid(customs)) {
                        nonSolidFree = true;
                        break;
                    }
                }

                if (nonSolidFree
                        && (dy <= 0.5
                        || buffer.start("upwards=" + AlgebraUtils.cut(dy, 3), 10) >= 2)) { // Step Blocks
                    reason = "upwards";
                    dm = cacheLimit(decimals, dm, upwards, 15);
                } else {
                    boolean ascension = false;

                    for (int i = 0; i >= -1; i--) {
                        for (SpartanLocation locs : custom.getSurroundingLocations(BlockUtils.hitbox, i, BlockUtils.hitbox)) {
                            if (BlockUtils.areStairs(locs) || BlockUtils.areSlabs(locs)) {
                                ascension = true;
                                break;
                            }
                        }
                    }

                    if (ascension) {
                        reason = "ascension";
                        dm = cacheLimit(decimals, dm, upwards, 15);
                    }
                }
            }
        }

        // Limit
        if (p.stepsOnBoats()) {
            if (d == od) {
                dm = cacheLimit(decimals, dm, ground_block_speed_extended, 5);
            } else {
                dm = cacheLimit(decimals, dm, ground_block_speed, 5);
            }
        } else {
            for (Entity entity : p.getNearbyEntities(3, 3, 3)) {
                if (entity instanceof Boat) {
                    reason = "entity";

                    if (d == od) {
                        dm = cacheLimit(decimals, dm, ground_block_speed_extended, 5);
                    } else {
                        dm = cacheLimit(decimals, dm, ground_block_speed, 5);
                    }
                    break;
                }
            }
        }

        // Limit
        if (dm != upwards && (dm == air || dm == ground_block_speed)
                && GroundUtils.isZeroMiscalculation(remaining) && dy == 0.0 && p.isSprinting()
                && cooldowns.canDo("skip-ground-jump-limit")) {
            reason = "ground";
            dm = cacheLimit(decimals, dm, ground_jump_speed, 10);
        }

        // Limit
        if (PatternUtils.isBlockPattern(new double[][]{
                {1, -remaining, 1},
                {BlockUtils.hitbox, -remaining, BlockUtils.hitbox}
        }, to, true, BlockUtils.bed)) {
            cooldowns.add("bed", 10);
            reason = "bed";
            dm = cacheLimit(decimals, dm, bed, 10);
        } else if (!cooldowns.canDo("bed")) {
            reason = "bed";
            dm = cacheLimit(decimals, dm, bed, 10);
        }

        // Limit
        if (extremelyPushed) {
            reason = "extreme-push";
            dm = cacheLimit(decimals, dm, pushExtreme, 15);
        } else if (fairlyPushed) {
            reason = "fair-push";
            dm = cacheLimit(decimals, dm, pushFair, 15);
        }

        // Limit
        if (piston || (dm == ice || dm == air || dm == ypos_advanced) && MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)
                && (to.isBlock(Material.SLIME_BLOCK, 1) || loc1.isBlock(Material.SLIME_BLOCK, 1))) {
            dm = cacheLimit(decimals, dm, Speed.piston, 5);
        }

        // Limit
        if (selfHit || FishingHook.hasCooldown(p)) {
            dm = cacheLimit(decimals, dm, Speed.selfHit, 5);
        }

        // Limit
        if (!PatternUtils.isBlockPattern(aboveBlock, to, true, BlockUtils.solid)) {
            cooldowns.add("above-block", 20);
        }
        return new Object[]{dm, reason, iceBlocks};
    }

    private static double adjustLimit(SpartanPlayer p, SpartanLocation to, String reason, double dm, float difference,
                                      boolean hasTrident, boolean bowProt, boolean hitDelay, boolean explosion, boolean selfHit, boolean damage,
                                      double minimumLimit) {
        double dm_speed = dm + (difference * 1.48);

        if (PlayerData.hasLowJumpEffect(p)) {
            if (reason.equals("air")) {
                dm_speed += 0.26;
            } else {
                dm_speed += 0.5;
            }
        } else if (PlayerData.hasHighJumpEffect(p) && dm_speed != sneaking && dm_speed != walking) {
            double value = dm_speed == ice || dm_speed == blocks_ice ? 0.4 : 0.275;
            dm_speed += value;
        }

        // Separator
        if (hitDelay || explosion) {
            dm_speed = Math.max(dm_speed + 0.5, 1.0);
        } else if (damage && Damage.getLastReceived(p) <= 1000L) {
            dm_speed += 0.3;
        }
        if (CrackShot.isUsingScope(p) || selfHit && dm > Speed.selfHit) {
            dm_speed += 0.3;
        }
        if (dm == air && Compatibility.CompatibilityType.MythicMobs.isFunctional()) {
            dm_speed += 0.075;
        }

        // Separator
        double averageSprintingSpeedDifference = getAverageSprintingSpeedDifference();

        if (averageSprintingSpeedDifference >= 0.1) {
            dm_speed += averageSprintingSpeedDifference;
        }

        // Separator
        if (hasTrident) {
            dm_speed = Math.max(trident, dm_speed);
        } else if (bowProt) {
            dm_speed = Math.max(bow, dm_speed);
        } else {
            dm_speed = MoveUtils.calculateLimit(p, to, dm_speed, 4.0, PotionEffectType.SPEED);
        }
        return Math.max(minimumLimit, dm_speed);
    }

    static boolean isSolid(SpartanLocation loc, double remaining) {
        Material l1 = loc.getBlock().getType();

        // Certain (Phase 1)
        if (BlockUtils.areCarpets(l1) || BlockUtils.areTrapdoors(l1)) {
            return true;
        }
        Material l2 = loc.clone().add(0, 2, 0).getBlock().getType();

        // Certain (Phase 2)
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && l2 == Material.END_ROD || BlockUtils.areCarpets(l2) || BlockUtils.areTrapdoors(l2)) {
            return true;
        }
        boolean can = false;

        for (double i = (2.0 - remaining); i <= (remaining >= 0.8 ? (3.0 - remaining) : 2.0); i++) {
            if (BlockUtils.isSolid(loc.clone().add(0, i, 0))) {
                return true;
            }
            for (SpartanLocation directionalLoc : loc.getSurroundingLocations(1, i, 1)) {
                if (BlockUtils.isSolid(directionalLoc)) {
                    can = true;
                    break;
                }
            }
            if (can) {
                break;
            }
        }
        if (can) {
            for (int i = 0; i <= (1 - remaining); i++) {
                for (SpartanLocation directionalLoc : loc.getSurroundingLocations(1, i, 1)) {
                    if (BlockUtils.isFullSolid(directionalLoc)) {
                        return false;
                    }
                }
            }
        }
        return can;
    }

    private static double getAverageSprintingSpeedDifference() {
        if (SpartanBukkit.isProductionServer()) {
            List<SpartanPlayer> players = SpartanBukkit.getPlayers();

            if (players.size() > 0) {
                String key = "average=sprinting=speed";
                double summary = 0.0;
                int count = 0;

                for (SpartanPlayer p : players) {
                    double d = p.getDecimals(check).get(key, 0.0);

                    if (d != 0.0 && !p.getProfile().isSuspectedOrHacker(check)) {
                        count++;
                        summary += d;
                    }
                }
                if (count > 0) {
                    return (summary / ((double) count)) - MovementProcessing.sprintingMaxSpeed;
                }
            }
        }
        return 0.0;
    }

    static boolean canDo(SpartanPlayer p, SpartanLocation to) { // Attention, part of the Sprint class too
        return p.canRunCheck(check, true)
                && !p.getHandlers().has(Handlers.HandlerType.Vehicle)
                && !p.getHandlers().has(Handlers.HandlerType.ElytraUse)
                && !MoveUtils.isInWaterTunnel(p, to);
    }

    private static double cacheLimit(Decimals decimals, double existingLimit, double newLimit, int ticks) {
        if (newLimit > existingLimit) {
            existingLimit = newLimit;
        }
        if (ticks > 0) {
            double repetitiveLimit = decimals.get("repetitive-limit", 0.0);

            if (existingLimit >= repetitiveLimit) {
                if (existingLimit != repetitiveLimit) {
                    decimals.set("repetitive-limit", existingLimit);
                }
                if (ticks >= decimals.get("ticks", 0.0)) {
                    decimals.set("ticks", ticks);
                }
            }
        }
        return existingLimit;
    }

    private static double getGreaterRepetitiveLimit(Decimals decimals) {
        return decimals.get("repetitive-limit", 0.0);
    }

    private static double getLimitTicks(Decimals decimals) {
        return decimals.get("ticks");
    }
}
