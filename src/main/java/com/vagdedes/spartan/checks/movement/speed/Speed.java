package com.vagdedes.spartan.checks.movement.speed;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.data.Cooldowns;
import com.vagdedes.spartan.abstraction.data.Decimals;
import com.vagdedes.spartan.abstraction.data.Handlers;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.abilities.crackshot.CrackShot;
import com.vagdedes.spartan.compatibility.manual.damage.NoHitDelay;
import com.vagdedes.spartan.functionality.identifiers.complex.predictable.BouncingBlocks;
import com.vagdedes.spartan.functionality.identifiers.complex.predictable.FloorProtection;
import com.vagdedes.spartan.functionality.identifiers.complex.unpredictable.Damage;
import com.vagdedes.spartan.functionality.identifiers.simple.DetectionLocation;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import com.vagdedes.spartan.utils.gameplay.PatternUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Speed extends CheckExecutor {

    public static final HackType check = HackType.Speed;
    private final SprintSpeed sprintDetection;
    private final WaterSpeed waterDetection;

    public Speed(SpartanPlayer player) {
        super(check, player, 2);
        this.addDetections(new DetectionExecutor[]{
                new SprintSpeed(this),
                new WaterSpeed(this)
        });
        sprintDetection = (SprintSpeed) detections[0];
        waterDetection = (WaterSpeed) detections[1];
    }

    public static final byte
            SPRINT = 0,
            WATER = 1;
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
    }, belowBlocks = new double[][]{
            {BlockUtils.hitbox, -1, BlockUtils.hitbox},
            {BlockUtils.hitbox, 0, BlockUtils.hitbox}
    }, iceLocations = new double[][]{
            {0, -1, 0},
            {0, 0, 0}
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
        float f = (float) PlayerUtils.gravityAcceleration;
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

    public static void updateLimits() {
        air = Compatibility.CompatibilityType.ViaRewind.isFunctional() ? 0.39 : air;
    }

    @Override
    public boolean handleInternal(boolean cancelled, Object object) {
        if (object.equals(WATER)) {
            waterDetection.run();
        } else if (canDo()) {
            sprintDetection.run();
        }
        return false;
    }

    @Override
    public boolean runInternal(boolean cancelled) {
        Double hor = this.player.movement.getNmsHorizontalDistance();

        if (hor == null || hor < sneaking || player.movement.wasInLiquids() || !canDo()) {
            return false;
        }
        Double ver = this.player.movement.getNmsHorizontalDistance(),
                oldVer = this.player.movement.getPreviousNmsVerticalDistance(),
                oldHor = this.player.movement.getPreviousNmsHorizontalDistance(),
                box = this.player.movement.getNmsBox(),
                oldBox = this.player.movement.getPreviousNmsBox();

        if (ver == null
                || oldVer == null
                || oldHor == null
                || box == null
                || oldBox == null) {
            return false;
        }
        int air = this.player.movement.getTicksOnAir();
        SpartanLocation to = player.movement.getEventToLocation();
        Cooldowns cooldowns = player.getCooldowns(hackType);
        Decimals decimals = player.getDecimals(hackType);
        double dm = getGreaterRepetitiveLimit(decimals);
        String reason;
        boolean aboveBlocks = false,
                groundVanilla = player.isOnGround(),
                groundCustom = player.isOnGroundCustom(),
                jumping = player.movement.isJumping(ver),
                extremelyPushed = player.getHandlers().has(Handlers.HandlerType.ExtremeCollision),
                fairlyPushed = extremelyPushed || PlayerUtils.hasNearbyCollisions(player),
                piston = player.getHandlers().has(Handlers.HandlerType.Piston),
                honey = !PatternUtils.isBlockPattern(Speed.honey, to, true, BlockUtils.honey_block);
        SpartanLocation loc1 = to.clone().add(0, 1, 0),
                locM1 = to.clone().add(0, -1, 0);

        if (player.movement.isCrawling()
                || isSolid(box)
                && (PlayerUtils.isOnGround(player, to, 0, -box, 0)
                || PlayerUtils.isOnGround(player, to, 0, -(box * 2), 0))) {
            reason = "blocks";
            aboveBlocks = true;

            if (piston) {
                dm = cacheLimit(decimals, dm, blocks_ice, 15);
            } else if (player.getHandlers().has(Handlers.HandlerType.SensitiveBlockBreak) || BlockUtils.isSlime(player, to, 2)) {
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
                    {BlockUtils.hitbox, player.getEyeHeight() + PlayerUtils.lowPrecision, BlockUtils.hitbox},
            }, to, true, BlockUtils.web)) {
                reason = "cobweb";
                dm = cacheLimit(decimals, dm, cobweb, 5);
            } else if (PatternUtils.isBlockPattern(new double[][]{
                    {BlockUtils.hitbox, 0, BlockUtils.hitbox},
                    {BlockUtils.hitbox, 1, BlockUtils.hitbox},
                    {BlockUtils.hitbox, player.getEyeHeight() + PlayerUtils.lowPrecision, BlockUtils.hitbox},
            }, to, true, BlockUtils.interactive_bushes)) {
                reason = "interactive-bush";
                dm = cacheLimit(decimals, dm, interactiveBush, 5);
            } else if (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15) && player.movement.isSneaking()) {
                reason = "sneaking";
                dm = cacheLimit(decimals, dm, sneaking, 0);
            } else if ((material = to.getBlock().material) == Material.SOUL_SAND || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16) && material == Material.SOUL_SOIL) {
                reason = "soul-sand";

                if (PlayerUtils.hasSoulSpeedEnchantment(player, to)) {
                    dm = cacheLimit(decimals, dm, ground, 5);
                } else {
                    dm = cacheLimit(decimals, dm, soul_sand_ground, 5);
                }
            } else {
                material = locM1.getBlock().material;

                if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8) && material == Material.SLIME_BLOCK || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15) && material == Material.HONEY_BLOCK) {
                    dm = cacheLimit(decimals, dm, ground_slime, 0);
                } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15) || player.movement.isSprinting()) {
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
        } else if (Math.abs(PlayerUtils.jumping[1] - ver) < PlayerUtils.getJumpingPrecision(player)
                || Math.abs(PlayerUtils.jumping[1] - box) < PlayerUtils.getJumpingPrecision(player)) {
            reason = "ypos";
            if (!cooldowns.canDo("ice=reminder")) {
                dm = cacheLimit(decimals, dm, ypos_advanced, 0);
            } else {
                dm = cacheLimit(decimals, dm, ypos, 0);
            }
        } else if ((box >= 0.084 && box <= 0.15 || box >= 0.75 || GroundUtils.heightExists(box))
                && ver == 0.0 && !BlockUtils.isGroundFree(player, to, 0.2, 0, 0.2) && (groundVanilla || groundCustom)) {
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
            } else if (honey || BlockUtils.isSlime(player, to, 3)) {

                if (PatternUtils.isBlockPattern(aboveBlock, to, true, BlockUtils.solid, BlockUtils.semi_solid)) {
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
                if (!jumping && hor >= 0.33 && box > 0.0 && box < 0.1 && PlayerUtils.isOnGround(player, to, 0, -1, 0)) {
                    cooldowns.add("skip-ground-jump-limit", 10);
                }
                if (FloorProtection.hasCooldown(player)) {
                    dm = cacheLimit(decimals, dm, air_non_solid_min, 8);
                } else {
                    dm = cacheLimit(decimals, dm, Speed.air, 8);
                }
            }
        }

        // Limit
        boolean selfHit = Damage.hasCooldown(player, Damage.selfHitKey);
        Buffer buffer = player.getBuffer(hackType);
        Object[] dynamicLimit = getDynamicLimit(to, loc1, buffer, decimals, cooldowns,
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
        boolean bedrockPlayer = player.bedrockPlayer,
                vehicle = player.getHandlers().has(Handlers.HandlerType.Vehicle),
                hasTrident = player.getHandlers().has(Handlers.HandlerType.Trident),
                explosion = !hasTrident && player.getHandlers().has(Handlers.HandlerType.Explosion),
                damage = Damage.hasCooldown(player, null),
                knockback = Damage.hasCooldown(player, Damage.knockbackKey),
                hitDelay = damage && NoHitDelay.hasCooldown(player),
                incompatibilities = Compatibility.CompatibilityType.MajorIncompatibility.isFunctional();
        float difference = PlayerUtils.getWalkSpeedDifference(player);
        double minimumLimit = Math.max(
                bedrockPlayer || incompatibilities ? incompatibility :
                        player.getHandlers().has(Handlers.HandlerType.ElytraWear)
                                || Compatibility.CompatibilityType.MythicMobs.isFunctional()
                                || Compatibility.CompatibilityType.mcMMO.isFunctional() ? 0.6 :
                                0.0,
                hackType.getCheck().getDecimalOption("minimum_limit_in_blocks", 0.0)
        ), modifiedLimit = adjustLimit(to, reason, dm, difference, hasTrident, knockback, hitDelay, explosion, selfHit, damage, vehicle, minimumLimit),
                dmCopy = dm;
        sprintDetection.setLimit(modifiedLimit);

        if (player.movement.isSprinting() && (modifiedLimit == ground || modifiedLimit == Speed.air)) {
            decimals.set("average=sprinting=speed", hor);
        } else {
            decimals.remove("average=sprinting=speed");
        }

        if (dm != blocks && dm != blocks_slime && (reason.length() != 6 || !reason.equals("blocks"))
                && !incompatibilities && !bedrockPlayer && !vehicle && !damage && !extremelyPushed
                && !hasTrident && !hitDelay && !piston && difference == 0.0f && minimumLimit <= 0.0
                && !PlayerUtils.hasSpeedEffect(player, to, true) && !PlayerUtils.hasJumpEffect(player)
                && !PlayerUtils.hasLevitationEffect(player) && !FloorProtection.hasCooldown(player)
                && !player.getHandlers().has(Handlers.HandlerType.WaterElevator)) {
            boolean aboveBlocksCopy = aboveBlocks;

            runAsync(() -> {
                boolean building = player.getHandlers().has(Handlers.HandlerType.TowerBuilding);

                // Separator
                if (modifiedLimit == Speed.air && !honey && !explosion && hor >= air_decreased
                        && buffer.start("value-decrease", 50) == 3) {
                    buffer.remove("value-decrease");
                    cancel("type: air(decreased), horizontal: " + hor, DetectionLocation.get(player, true));
                }

                //Separator
                if (ver > 0.0 && air <= 12
                        && !explosion
                        && !jumping
                        && (dmCopy == Speed.air || dmCopy == ground || dmCopy == ground_jump_speed || dmCopy == ground_block_speed
                        || dmCopy == ground_block_speed_extended || dmCopy == sneaking || dmCopy == walking)
                        && (!groundVanilla || !groundCustom)
                        && !BouncingBlocks.isBelow(player)
                        && !BlockUtils.isSlime(player, to, 2)
                        && !PatternUtils.isBlockPattern(hopIllegalSingle, to, true, BlockUtils.solid, BlockUtils.semi_solid)
                        && !PatternUtils.isBlockPattern(hopIllegal, to, true, BlockUtils.semi_solid)
                        && !BlockUtils.isSlime(player, to, 3) && buffer.start("hop=illegal", 10) >= 4) {
                    cancel("type: hop(illegal), horizontal: " + hor + ", limit: " + dmCopy, DetectionLocation.get(player, true));
                }

                //Separator
                if (dmCopy == upwards) {
                    double diff = Math.abs(hor - oldHor);

                    if (hor > oldHor && diff >= 0.35 && ver == 0.5) {
                        cancel("type: upwards(difference), horizontal: " + hor + ", old-horizontal: " + oldHor + ", difference: " + diff, DetectionLocation.get(player, true));
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
                            cancel("type: ice-speed(blocks), difference: " + diff + ", horizontal: " + hor + ", old-horizontal: " + oldHor, DetectionLocation.get(player, true));
                        } else if (iceTicks >= 20 && hor >= 0.5 && (diff >= 0.25 || diff <= 0.15)) {
                            cancel("type: ice-speed(normal), difference: " + diff + ", horizontal: " + hor + ", old-horizontal: " + oldHor, DetectionLocation.get(player, true));
                        }
                    }
                }

                // Separator
                if (dmCopy == ice && !aboveBlocksCopy && !piston) {
                    if (hor >= 0.5 && buffer.start("ice_speed=counter", 20) >= 7) {
                        cancel("type: ice-speed(counter), horizontal: " + hor + ", limit: " + dmCopy, DetectionLocation.get(player, true));
                    }
                    if (groundCustom && hor >= (dmCopy * (2.0 / 3.0))
                            && buffer.canDo(reasonCopy)
                            && !PatternUtils.isBlockPattern(genericHitbox, to, true, BlockUtils.semi_solid)
                            && PatternUtils.isBlockPattern(minusOne, to, true, BlockUtils.ice)
                            && buffer.start("ice_speed", 10) == 4) {
                        buffer.remove("ice_speed");
                        cancel("type: ice-speed(ground), horizontal: " + hor + ", limit: " + dmCopy, DetectionLocation.get(player, true));
                    }
                }

                // Separator
                if (!aboveBlocksCopy
                        && hor < modifiedLimit
                        && (dmCopy == Speed.air || dmCopy == walking || dmCopy == ground || dmCopy == sneaking || dmCopy == ground_jump_speed)
                        && cooldowns.canDo("above-block")
                        && cooldowns.canDo("bed")
                        && !BlockUtils.isSlime(player, to, 2)) {
                    float fallDistance = player.getFallDistance();

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

                        if (hor >= 0.215 && illegal && !fairlyPushed && air <= 12 && !player.isOnFire() && !BouncingBlocks.isBelow(player) && !building) {
                            cancel("type: fall-distance(illegal), horizontal: " + hor + ", limit: " + dmCopy + ", fall-distance: " + fallDistance + ", a: " + air + ", box: " + box + ", vertical: " + ver, DetectionLocation.get(player, true));
                        } else if (hor > 0.335 && fallDistance > 0.0f && (!groundVanilla || !groundCustom) && !BlockUtils.isSemiSolid(locM1)) {
                            cancel("type: fall-distance(normal), horizontal: " + hor + ", limit: " + dmCopy + ", box: " + box + ", vertical: " + ver, DetectionLocation.get(player, true));

                        } else if (groundCustom && hor >= 0.36 && fallDistance == 0.0f && box == 0.0) {
                            if (buffer.start("fall-distance", 10) == 2) {
                                buffer.remove("fall-distance");
                                cancel("type: fall-distance(ground), horizontal: " + hor + ", limit: " + dmCopy + ", box: " + box + ", vertical: " + ver, DetectionLocation.get(player, true));
                            }
                        }
                    }
                }

                // Separator
                if (dmCopy == Speed.air && hor >= 0.34 && hor < modifiedLimit && (!groundVanilla || !groundCustom)
                        && buffer.start("hop=motion", 20) == 12) {
                    buffer.remove("hop=motion");
                    cancel("type: hop(motion), horizontal: " + hor + ", limit: " + dmCopy, DetectionLocation.get(player, true));
                }

                // Separator
                if (dmCopy == ground_jump_speed && hor >= 0.36 && hor < modifiedLimit && ver <= 0.0 && !aboveBlocksCopy
                        && buffer.start("ground_jump_speed", 10) == 3) {
                    buffer.remove("ground_jump_speed");
                    cancel("type: ground(jump-speed), horizontal: " + hor + ", limit: " + dmCopy, DetectionLocation.get(player, true));
                }

                // Separator
                if (hor >= 0.295 && hor < modifiedLimit && !aboveBlocksCopy && !explosion && !building) {
                    boolean allow = false;
                    double v = AlgebraUtils.cut(hor, 8);

                    if (dmCopy == Speed.air && (!groundVanilla || !groundCustom) && PlayerUtils.isOnGround(player, to, 0, -0.25, 0)) {
                        allow = true;
                    } else if (dmCopy == ground || dmCopy == walking || dmCopy == sneaking) {
                        allow = true;
                    }

                    if (allow
                            && !PatternUtils.isBlockPattern(groundLocations, to, true, BlockUtils.semi_solid)
                            && buffer.start("high_normal_speed", 20) >= 18) {
                        cancel("type: ground(high), horizontal: " + hor + ", limit: " + dmCopy, DetectionLocation.get(player, true));
                    }
                    long time = player.getTimer(hackType).get("repeated=" + v);

                    if (time <= 250L && buffer.start("repeated", 10) == 3) {
                        buffer.remove("repeated");
                        cancel("type: repeated, t: " + time + ", horizontal: " + v + ", limit: " + dmCopy, DetectionLocation.get(player, true));
                    }
                    player.getTimer(hackType).set("repeated=" + v);
                }

                // Separator
                if (dmCopy == ypos && reasonCopy.equals("ypos") && hor >= 0.4 && hor < modifiedLimit
                        && buffer.start("ypos=attempts", 20) == 4) {
                    buffer.remove("ypos=attempts");
                    cancel("type: ypos, horizontal: " + hor + ", limit: " + dmCopy + ", dynamic-limit: " + modifiedLimit, DetectionLocation.get(player, true));
                }
            });
        }

        // Detection
        if (modifiedLimit > 0.0 && hor >= modifiedLimit) {
            runAsync(() -> {
                boolean allow = true;

                // Separator
                if (groundCustom || PlayerUtils.isOnGround(player, to, 0, -1, 0)) {
                    if (modifiedLimit == ground && hor < 0.36
                            || (modifiedLimit == Speed.air || modifiedLimit == ground_block_speed || modifiedLimit == ground_block_speed_extended) && hor < 0.63
                            || modifiedLimit == sneaking && hor < 0.2) {
                        long m = player.getTimer(hackType).get("limit=protection");

                        if (player.getTimer(hackType).has(m)) {
                            if (m >= 4000L) {
                                allow = false;
                            }
                        } else {
                            allow = false;
                        }
                    }
                    player.getTimer(hackType).set("limit=protection");
                }

                if (allow) {
                    int time, attempts;

                    if (hitDelay
                            || explosion
                            || damage && (player.getLastDamageReceived().ticksPassed() <= 10 || Damage.hasCooldown(player, Damage.knockbackKey))) {
                        time = 10;
                        attempts = 6;
                    } else if (PlayerUtils.hasHighJumpEffect(player)) {
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
                        cancel("type: " + strongPrevention + ", reason: " + reasonCopy + ", horizontal: " +
                                hor + ", limit: " + dmCopy + ", dynamic-limit: " + modifiedLimit + ", vertical: " + ver + ", air: " + air
                                + ", difference: " + Math.abs(hor - oldHor), DetectionLocation.get(player, true));
                    }
                }
            });
        }
        return false;
    }

    private Object[] getDynamicLimit(SpartanLocation to, SpartanLocation loc1, Buffer buffer, Decimals decimals, Cooldowns cooldowns,
                                     double hor, double oHor, double ver, double box, double oBox, int airTicks,
                                     boolean jumping, boolean extremelyPushed, boolean fairlyPushed, boolean aboveBlocks, boolean groundCustom, boolean selfHit, boolean piston,
                                     double dm, String reason) {
        boolean iceBlocks = false;

        // Limit
        if (PatternUtils.isBlockPattern(iceLocations, to, false, BlockUtils.ice)) {
            cooldowns.add("ice=reminder", 20);
            int iceBlockTimes = 15;

            if (!aboveBlocks) {
                iceBlocks = true;
                dm = cacheLimit(decimals, dm, ice, 0);

                if (dm == ice) {
                    reason = "ice";
                    dm = cacheLimit(decimals, dm, dm, iceBlockTimes);
                }
            } else {
                dm = cacheLimit(decimals, dm, blocks_ice, iceBlockTimes * 2);
            }
        }

        // Limit
        if ((player.movement.isSprinting() || player.movement.isSprintJumping())
                && (jumping || ver == 0.375 || ver >= 0.5 && ver < 0.6 || Math.abs(box - oBox) == 0.5)

                && (groundCustom
                || PlayerUtils.isOnGround(player, to, 0, -1, 0)
                || PlayerUtils.isOnGround(player, to, 0, -1.5, 0))) {
            // Combined Step Blocks (Not using hitbox_max due to ascension imperfections)
            if ((dm == air || dm == air_non_solid_min || dm == air_non_solid_max) && airTicks == 0 && (ver > 0.5 || jumping)
                    && PatternUtils.isBlockPattern(belowBlocks, to, true, BlockUtils.semi_solid)) {
                reason = "upwards(combined)";
                dm = cacheLimit(decimals, dm, upwards, 10);
            } else if (!jumping) {
                SpartanLocation custom = to.clone();
                custom.setPitch(0.0f);
                custom = custom.add(to.getDirection().multiply(1.0));
                Boolean nonSolidFree = PatternUtils.getCached(custom, hackType, "upwards");

                if (nonSolidFree == null) {
                    for (SpartanLocation customs : custom.getSurroundingLocations(BlockUtils.hitbox, 0, BlockUtils.hitbox)) {
                        if (BlockUtils.isFullSolid(customs)) {
                            nonSolidFree = true;
                            break;
                        }
                    }
                    nonSolidFree = PatternUtils.cache(custom, hackType, "upwards", nonSolidFree != null);
                }

                if (nonSolidFree
                        && (ver <= 0.5
                        || buffer.start("upwards=" + AlgebraUtils.cut(ver, 3), 10) >= 2)) { // Step Blocks
                    reason = "upwards";
                    dm = cacheLimit(decimals, dm, upwards, 15);
                } else if (PatternUtils.isBlockPattern(belowBlocks, custom, true, BlockUtils.slabs, BlockUtils.stairs)) {
                    reason = "ascension";
                    dm = cacheLimit(decimals, dm, upwards, 15);
                }
            }
        }

        // Limit
        if (player.movement.stepsOnBoats()) {
            if (hor == oHor) {
                dm = cacheLimit(decimals, dm, ground_block_speed_extended, 5);
            } else {
                dm = cacheLimit(decimals, dm, ground_block_speed, 5);
            }
        } else {
            List<Entity> entities = player.getNearbyEntities(3, 3, 3);

            if (!entities.isEmpty()) {
                for (Entity entity : entities) {
                    if (entity instanceof Boat) {
                        reason = "entity";

                        if (hor == oHor) {
                            dm = cacheLimit(decimals, dm, ground_block_speed_extended, 5);
                        } else {
                            dm = cacheLimit(decimals, dm, ground_block_speed, 5);
                        }
                        break;
                    }
                }
            }
        }

        // Limit
        if (dm != upwards && (dm == air || dm == ground_block_speed)
                && box == 0.0 && ver == 0.0 && player.movement.isSprinting()
                && cooldowns.canDo("skip-ground-jump-limit")) {
            reason = "ground";
            dm = cacheLimit(decimals, dm, ground_jump_speed, 10);
        }

        // Limit
        if (PatternUtils.isBlockPattern(new double[][]{
                {1, -box, 1},
                {BlockUtils.hitbox, -box, BlockUtils.hitbox}
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
        if (selfHit) {
            dm = cacheLimit(decimals, dm, Speed.selfHit, 5);
        }

        // Limit
        if (!PatternUtils.isBlockPattern(aboveBlock, to, true, BlockUtils.solid, BlockUtils.semi_solid)) {
            cooldowns.add("above-block", 20);
        }
        return new Object[]{dm, reason, iceBlocks};
    }

    private double adjustLimit(SpartanLocation to, String reason, double dm, float difference,
                               boolean hasTrident, boolean bowProt, boolean hitDelay, boolean explosion, boolean selfHit, boolean damage, boolean vehicle,
                               double minimumLimit) {
        double dm_speed = dm + (difference * 1.48);

        if (PlayerUtils.hasLowJumpEffect(player)) {
            if (reason.equals("air")) {
                dm_speed += 0.26;
            } else {
                dm_speed += 0.5;
            }
        } else if (PlayerUtils.hasHighJumpEffect(player) && dm_speed != sneaking && dm_speed != walking) {
            double value = dm_speed == ice || dm_speed == blocks_ice ? 0.4 : 0.275;
            dm_speed += value;
        }

        // Separator
        if (hitDelay || explosion) {
            dm_speed = Math.max(dm_speed + 0.5, 1.0);
        } else if (damage && player.getLastDamageReceived().ticksPassed() <= 20) {
            dm_speed += 0.3;
        }
        if (CrackShot.isUsingScope(player) || selfHit && dm > Speed.selfHit) {
            dm_speed += 0.3;
        }
        if (dm == air && Compatibility.CompatibilityType.MythicMobs.isFunctional()) {
            dm_speed += 0.075;
        }

        // Separator
        if (hasTrident) {
            dm_speed = Math.max(trident, dm_speed);
        } else if (bowProt) {
            dm_speed = Math.max(bow, dm_speed);
        } else {
            dm_speed = PlayerUtils.calculateLimit(player, to, dm_speed, 4.0, PotionEffectType.SPEED);
        }
        if (vehicle) {
            Entity entity = player.getVehicle();

            if (entity != null) { // In case the player recently exited their vehicle
                if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_20)
                        && entity instanceof Camel) {
                    minimumLimit = Math.max(minimumLimit, 2.5);
                } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_11)
                        && (entity instanceof Horse || entity instanceof Llama)) {
                    minimumLimit = Math.max(minimumLimit, 1.25);
                } else if (entity instanceof Boat) {
                    minimumLimit = Math.max(minimumLimit, 0.45);
                } else if (entity instanceof Pig) {
                    minimumLimit = Math.max(minimumLimit, 0.3);
                }
            }
        }
        return Math.max(minimumLimit, dm_speed);
    }

    private boolean isSolid(double box) {
        SpartanLocation to = player.movement.getEventToLocation();
        Boolean pattern = PatternUtils.getCached(to, hackType, "solid");

        if (pattern != null) {
            return pattern;
        }
        Material l1 = to.getBlock().material;

        // Certain (Phase 1)
        if (BlockUtils.areCarpets(l1) || BlockUtils.areTrapdoors(l1)) {
            return PatternUtils.cache(to, hackType, "solid", true);
        }
        Material l2 = to.clone().add(0, 2, 0).getBlock().material;

        // Certain (Phase 2)
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && l2 == Material.END_ROD
                || BlockUtils.areCarpets(l2)
                || BlockUtils.areTrapdoors(l2)) {
            return PatternUtils.cache(to, hackType, "solid", true);
        }
        boolean can = false;

        for (double i = (2.0 - box); i <= (box >= 0.8 ? (3.0 - box) : 2.0); i++) {
            SpartanLocation[] locations = to.getSurroundingLocations(1, i, 1);

            for (int x = 0; x < locations.length; x++) {
                SpartanLocation loopLocation = locations[x];

                if (BlockUtils.isSolid(loopLocation)) {
                    if (x == 0) {
                        return PatternUtils.cache(to, hackType, "solid", true);
                    } else {
                        can = true;
                        break;
                    }
                }
            }
            if (can) {
                break;
            }
        }
        if (can) {
            for (int i = 0; i <= (1 - box); i++) {
                for (SpartanLocation directionalLoc : to.getSurroundingLocations(1, i, 1)) {
                    if (BlockUtils.isFullSolid(directionalLoc)) {
                        return PatternUtils.cache(to, hackType, "solid", false);
                    }
                }
            }
            return PatternUtils.cache(to, hackType, "solid", true);
        }
        return PatternUtils.cache(to, hackType, "solid", false);
    }

    @Override
    protected boolean canDo() {
        return player.canRunCheck(hackType, true)
                && !player.getHandlers().has(Handlers.HandlerType.ElytraUse)
                && !PlayerUtils.isInWaterTunnel(player, player.movement.getEventToLocation());
    }

    private double cacheLimit(Decimals decimals, double existingLimit, double newLimit, int ticks) {
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

    private double getGreaterRepetitiveLimit(Decimals decimals) {
        return decimals.get("repetitive-limit", 0.0);
    }

    private double getLimitTicks(Decimals decimals) {
        return decimals.get("ticks");
    }
}
