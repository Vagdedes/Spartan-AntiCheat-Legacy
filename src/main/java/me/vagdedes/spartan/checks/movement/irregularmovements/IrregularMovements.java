package me.vagdedes.spartan.checks.movement.irregularmovements;

import me.vagdedes.spartan.compatibility.manual.damage.NoHitDelay;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.features.performance.AsyncDetection;
import me.vagdedes.spartan.features.protections.Building;
import me.vagdedes.spartan.features.protections.Explosion;
import me.vagdedes.spartan.features.synchronicity.SpartanEdition;
import me.vagdedes.spartan.handlers.identifiers.BouncingBlocks;
import me.vagdedes.spartan.handlers.identifiers.Damage;
import me.vagdedes.spartan.handlers.identifiers.FishingHook;
import me.vagdedes.spartan.handlers.identifiers.FloorProtection;
import me.vagdedes.spartan.handlers.stability.TPS;
import me.vagdedes.spartan.handlers.tracking.MovementProcessing;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.data.Cooldowns;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.data.Timer;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.objects.system.hackPrevention.HackPrevention;
import me.vagdedes.spartan.objects.system.hackPrevention.HackPreventionUtils;
import me.vagdedes.spartan.system.Cache;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.utils.gameplay.*;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import org.bukkit.potion.PotionEffectType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class IrregularMovements {

    // Base
    public static final HackType check = HackType.IrregularMovements;
    private static final Map<UUID, SpartanLocation> oldFrom = Cache.store(new LinkedHashMap<>(Config.getMaxPlayers()));

    // Keys
    private static final String
            changeScenario = "changed-scenario",
            levitationKey = "levitation",
            waterElevatorKey = "water-elevator";
    static final String liquidsKey = "liquids";

    // Detections
    private static final String
            horizontalMotionDetection = "horizontal-motion",

    greaterFallDetection = "greater-fall",
            identicalFallDetection = "identical-fall",
            distantFallDetection = "distant-fall",
            slowFallDetection = "slow-fall",
            repeatedFallDetection = "repeated-fall",
            positionFallDetection = "position-fall",
            illegalFallDetection = "illegal-fall",

    greaterDownDetection = "greater-down",
            identicalDownDetection = "identical-down",
            distantDownDetection = "distant-down",
            slowDownDetection = "slow-down",
            repeatedDownDetection = "repeated-down",
            positionDownDetection = "position-down",
            illegalDownDetection = "illegal-down",

    greaterJumpDetection = "greater-jump",
            identicalJumpDetection = "identical-jump",
            distantJumpDetection = "distant-jump",
            unknownJumpDetection = "unknown-jump",
            airJumpDetection = "air-jump",
            illegalJumpDetection = "illegal-jump",
            spoofJumpDetection = "spoof-jump",
            uncompletedJumpDetection = "uncompleted-jump";
    private static final String[] detections = new String[]{
            horizontalMotionDetection,
            greaterFallDetection, identicalFallDetection, distantFallDetection, slowFallDetection,
            positionFallDetection, repeatedFallDetection, illegalFallDetection,
            greaterDownDetection, identicalDownDetection, distantDownDetection, slowDownDetection,
            positionDownDetection, repeatedDownDetection, illegalDownDetection,
            greaterJumpDetection, identicalJumpDetection, distantJumpDetection, unknownJumpDetection,
            uncompletedJumpDetection, airJumpDetection, spoofJumpDetection, illegalJumpDetection
    };

    private static final double[][]
            interactiveAndPassable = new double[][]{
            {0, 0, 0},
            {0, 1, 0},
    }, liquid = new double[][]{
            {0, 0, 0},
            {0, 1, 0},
    };
    private static final double[]
            genericHitbox = new double[]{
            BlockUtils.hitbox, 0, BlockUtils.hitbox
    };

    // Detections
    static final int defaultBufferTime = 20;
    private static final int minuteTicks = 1200;
    private static final int offGroundTicks = 12;
    static final int fullOffGroundTicks = 20;

    // Math
    static final int desiredDecimalAccuracy = (8 + 16) / 2;

    public static void remove(SpartanPlayer p) {
        oldFrom.remove(p.getUniqueId());
        IrregularMovementsStep.remove(p);
    }

    public static void refresh(SpartanPlayer p, SpartanLocation loc) {
        oldFrom.put(p.getUniqueId(), loc);
        IrregularMovementsStep.refresh(p, loc);
    }

    // Re-enable Fly & IrregularMovements checks
    public static void run(SpartanPlayer p, SpartanLocation to, SpartanLocation from,
                           double vertical, double oldVertical, double horizontal, double box, double oldBox,
                           int airTicks) {
        boolean bedrockPlayer = p.isBedrockPlayer();

        if (canDo(p, to, bedrockPlayer)) {
            // Scenario Management
            double allowedUp = 0.0, allowedDown = 0.0;
            boolean canGoUp, canGoDown,
                    canJump, canFall,
                    defaultScenario = false, groundInteractionScenario = false, damageScenario = false, // Real
                    abstractScenario = false, velocityScenario = false; // Combinations
            String scenario;

            // Detection Management
            boolean constructDetection = false,
                    checkUp = false, checkDown = false,
                    checkJump = false, checkFall = false,
                    checkHorizontal = false;

            // Limits Management
            double minimumUp = 0.0,
                    minimumDown = 0.0;

            // Early Variables
            boolean liquidBlocks,
                    waterElevator = p.getHandlers().has(Handlers.HandlerType.WaterElevator);

            if (waterElevator
                    || PatternUtils.isBlockPattern(liquid, to, false, BlockUtils.liquid)) {
                liquidBlocks = true;
            } else if (vertical < 0.0) {
                if (BlockUtils.isLiquid(to.clone().add(0, vertical, 0))) {
                    liquidBlocks = true;
                } else {
                    Boolean result = PatternUtils.getCached(to, check, "liquid-blocks");

                    if (result == null) {
                        for (double i = -1.0; i > Math.floor(vertical); i--) {
                            if (BlockUtils.isLiquid(to.clone().add(0, i, 0))) {
                                result = true;
                                break;
                            }
                        }
                        result = PatternUtils.cache(to, check, "liquid-blocks", result != null);
                    }
                    liquidBlocks = result;
                }
            } else {
                liquidBlocks = false;
            }

            UUID uuid = p.getUniqueId();
            SpartanLocation previousFrom = oldFrom.get(uuid);
            oldFrom.put(uuid, from);

            // Allow check to run when all cache is collected
            if (previousFrom == null) {
                return;
            }

            // Final Variables
            Buffer buffers = p.getBuffer(check);
            int blockY = to.getBlockY();
            boolean levitationEffect = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && PlayerData.hasLevitationEffect(p),
                    incompatibility = Compatibility.CompatibilityType.MajorIncompatibility.isFunctional() || Compatibility.CompatibilityType.ItemAttributes.isFunctional(),
                    fullAirTicksOffGround = airTicks >= fullOffGroundTicks,
                    airTicksOffGround = fullAirTicksOffGround || airTicks >= (bedrockPlayer || incompatibility ? fullOffGroundTicks : offGroundTicks),
                    onGroundCustom = p.isOnGroundCustom(),
                    offGround = !onGroundCustom || !p.isOnGround() && !liquidBlocks,
                    knownBoxes = GroundUtils.heightExists(box, true) || GroundUtils.heightExists(oldBox, true),
                    bouncingBlocks = (!knownBoxes || airTicks > 0) && BouncingBlocks.isBelow(p),
                    jumpEffect = p.hasPotionEffect(PotionEffectType.JUMP),
                    cachedJumpEffect = jumpEffect || PlayerData.hasJumpEffect(p),
                    slowFalling = PlayerData.hasSlowFallingEffect(p),
                    threwTrident = p.getHandlers().has(Handlers.HandlerType.Trident),
                    explosion = p.getHandlers().has(Handlers.HandlerType.Explosion),
                    falling = p.isFalling(vertical),
                    oldFalling = p.isFalling(oldVertical),
                    jumping = p.isJumping(vertical),
                    oldJumping = p.isJumping(oldVertical),
                    wasInLiquids = p.wasInLiquids();

            // Cache Variables
            Cooldowns cooldowns = p.getCooldowns(check);
            boolean heightExists = cacheBoolean(cooldowns, knownBoxes, "height-exists", 20),
                    heightExistsHalf = cacheBoolean(cooldowns, knownBoxes, "height-exists=half", 10),
                    building = cacheBoolean(cooldowns, Building.hasCooldown(p) || p.getHandlers().has(Handlers.HandlerType.BlockPlace), "building", 20),
                    badTPS = cacheBoolean(cooldowns, TPS.get(p) < TPS.good, "bad-TPS", 10),
                    pastFalling = cacheBoolean(cooldowns, falling || oldFalling, "past-falling", 20),
                    blockedJumping = cacheBoolean(cooldowns, GroundUtils.isBlockedJumping(p, to, cooldowns, vertical, box, airTicks), "blocked-jumping", 10);

            // Combination Variables
            boolean forcefulUp = levitationEffect || waterElevator || Explosion.justExploded(p),
                    forcefulUpExtended = forcefulUp || threwTrident; // Used to prevent false positives and not heavily modify detections

            // Buffer Reset
            if (onGroundCustom) {
                if (buffers.get("detection-reset", 1) == 1) {
                    buffers.set("detection-reset", 0);

                    for (String detection : detections) {
                        buffers.remove(new String[]{detection, detection + "=minute"});
                    }
                }
            } else {
                buffers.set("detection-reset", 1);
            }

            // Scenario Identification
            if (threwTrident
                    || PlayerData.hasNearbyCollisions(p)
                    || NoHitDelay.hasCooldown(p)
                    || p.getHandlers().has(Handlers.HandlerType.Piston)
                    || FishingHook.hasCooldown(p)) { // Extreme circumstances
                abstractScenario = true;
                velocityScenario = true;

                scenario = "abstract";
                canGoUp = true;
                canGoDown = !forcefulUp;
                canJump = !forcefulUp;
                canFall = !forcefulUp;
            } else if (liquidBlocks) { // Liquids & Water Elevators
                if (waterElevator) {
                    velocityScenario = true;
                    scenario = waterElevatorKey;
                    canJump = false;
                    minimumDown = -(MoveUtils.falling[0] * 7);
                } else {
                    scenario = liquidsKey;
                    canJump = true;
                    constructDetection = true;
                }
                canGoUp = true;
                canGoDown = !forcefulUp;
                canFall = !forcefulUp;
            } else if (explosion || Damage.hasCooldown(p)) { // Damage Or Combat
                damageScenario = true;
                velocityScenario = true;

                scenario = "damage/explosion";
                canGoUp = true;
                canGoDown = !forcefulUp;
                canJump = !forcefulUp;
                canFall = !forcefulUp;
            } else if (!p.isCrawling()

                    && (BlockUtils.isFullSolid(to)
                    && (!BlockUtils.isSolid(to.clone().add(0, 1, 0))
                    || BlockUtils.isFullSolid(to.clone().add(0, 1, 0)))

                    || BlockUtils.isFullSolid(to.clone().add(0, 1, 0))
                    && (!BlockUtils.isSolid(to)
                    || BlockUtils.isFullSolid(to)))) { // Inside Blocks
                scenario = "inside-blocks";
                canGoUp = true;
                canGoDown = !forcefulUp;
                canJump = !forcefulUp;
                canFall = !forcefulUp;

                constructDetection = true; // up
                checkHorizontal = true;
                checkJump = true;
                checkDown = true;
                checkFall = true;
            } else if (PatternUtils.isBlockPattern(new double[][]{
                    {BlockUtils.hitbox, 0, BlockUtils.hitbox},
                    {BlockUtils.hitbox, 1, BlockUtils.hitbox},
                    {BlockUtils.hitbox, -0.1, BlockUtils.hitbox},
                    {BlockUtils.hitbox, p.getEyeHeight() + MoveUtils.lowPrecision, BlockUtils.hitbox}
            }, to, true, BlockUtils.web)) { // Cobwebs
                scenario = "cobweb";
                canGoUp = forcefulUp;
                canGoDown = !forcefulUp;
                canJump = false;
                canFall = !forcefulUp;

                constructDetection = true; // all
                minimumUp = 0.022;
            } else if (PatternUtils.isBlockPattern(new double[][]{
                    {BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max},
                    {BlockUtils.hitbox_max, 1, BlockUtils.hitbox_max},
                    {BlockUtils.hitbox_max, -box, BlockUtils.hitbox_max},
                    {BlockUtils.hitbox_max, -1, BlockUtils.hitbox_max, onGroundCustom ? 1 : 0}
            }, to, true, BlockUtils.climbable)) { // Ladder/Vine Climbing
                scenario = "climb";
                canGoUp = true;
                canGoDown = !forcefulUp;
                canJump = !forcefulUp;
                canFall = !forcefulUp;

                constructDetection = true; // all
            } else if (jumpEffect) { // Jump Effect
                scenario = "jump-effect";
                canGoUp = true;
                canGoDown = !forcefulUp;
                canJump = !forcefulUp;
                canFall = !forcefulUp;
            } else if (bouncingBlocks) { // Slime/Bed Bouncing
                velocityScenario = true;

                scenario = "bouncing";
                canGoUp = true;
                canGoDown = !forcefulUp;
                canJump = !forcefulUp;
                canFall = !forcefulUp;

                constructDetection = true; // up, jump
                checkHorizontal = true;
                checkDown = true;
                checkFall = true;
            } else if (p.isOnFire()
                    || p.isFrozen()
                    || FloorProtection.hasCooldown(p)
                    || PlayerData.hasBadPotionEffects(p)
                    || PatternUtils.isBlockPattern(interactiveAndPassable, to, false, BlockUtils.interactive_and_passable)) { // Small damage by effects, circumstances or fire
                groundInteractionScenario = true;

                scenario = "ground-interaction";
                canGoUp = true;
                canGoDown = !forcefulUp;
                canJump = !forcefulUp;
                canFall = !forcefulUp;
            } else if (slowFalling) { // Slow Falling
                scenario = "slow-falling";
                canGoUp = true;
                canGoDown = !forcefulUp;
                canJump = !forcefulUp;
                canFall = !forcefulUp;
            } else if (levitationEffect) { // Levitation Effect
                abstractScenario = true;

                scenario = levitationKey;
                canGoUp = true;
                canGoDown = false;
                canJump = false;
                canFall = false;

                constructDetection = true; // all
            } else { // Default scenario
                scenario = "neutral";
                defaultScenario = true;
                canGoUp = true;
                canGoDown = true;
                canJump = true;
                canFall = true;
            }

            // Scenario Tracker
            int scenarioCode = Objects.hash(
                    scenario,
                    canGoUp, canGoDown, canJump, canFall,
                    checkUp, checkDown, checkJump, checkFall, checkHorizontal
            );
            boolean changedScenario = cacheBoolean(cooldowns, buffers.get(changeScenario, scenarioCode) != scenarioCode, "changed-scenario", 7);

            // Variable Modification
            if (waterElevator) { // Allow the player to go a bit down when in a water soul-sand elevator
                allowedDown = MoveUtils.falling[0] * 6;
            } else if (forcefulUp) { // Allow the player to go a bit down when in an upwards-only motion
                allowedDown = MoveUtils.falling[0] * 4;
            }
            if (forcefulUpExtended) { // Do not check upwards motion when the player is forcefully going up
                checkUp = false;
            }
            if (airTicksOffGround && velocityScenario) { // Attention to bedrockPlayer for the tick amount
                airTicksOffGround = fullAirTicksOffGround;
            }

            // Verbose/Detection Information
            String prevention = "scenario: " + scenario
                    + ", up/down: " + (canGoUp && canGoDown ? 1 : !canGoUp && !canGoDown ? 0 : (canGoUp ? 1 : 0) + "/" + (canGoDown ? 1 : 0))
                    + ", jump/fall: " + (canJump && canFall ? 1 : !canJump && !canFall ? 0 : (canJump ? 1 : 0) + "/" + (canFall ? 1 : 0))
                    + (vertical == oldVertical ? ", motion/old-motion: " + vertical : ", motion: " + vertical + ", old-motion: " + oldVertical)
                    + (box == oldBox ? ", box/old-box: " + box : ", box: " + box + ", old-box: " + oldBox)
                    + ", air: " + airTicks + ", horizontal: " + horizontal;

            // Do not process if the player is not in a water elevator or water block but has liquid cooldown
            if (!waterElevator && !liquidBlocks && wasInLiquids) {
                if (!velocityScenario) {
                    IrregularMovementsLiquids.run(p, to, from,
                            vertical, horizontal,
                            pastFalling, true, knownBoxes, building, jumping, oldJumping, changedScenario,
                            liquidsKey, prevention);
                }
                return;
            }

            if (constructDetection) {
                switch (scenario) {
                    case "climb":
                        if (!bedrockPlayer) {
                            IrregularMovementsClimb.run(p, to, from, cooldowns,
                                    vertical, oldVertical, box,
                                    knownBoxes, canGoUp, canGoDown, canJump, bouncingBlocks, jumping,
                                    prevention, scenario);
                        }
                        break;
                    case levitationKey:
                        if (!waterElevator && !liquidBlocks && canRunDetection(bedrockPlayer)) {
                            IrregularMovementsLevitation.run(p, to, from, cooldowns,
                                    vertical,
                                    canGoUp,
                                    box, oldBox,
                                    prevention, scenario);
                        }
                        break;
                    case "cobweb":
                        if (canRunDetection(bedrockPlayer)) {
                            IrregularMovementsCobweb.run(p, to, from,
                                    vertical, horizontal, box, oldBox,
                                    canGoDown,
                                    prevention, scenario);
                        }
                        break;
                    case "bouncing":
                        if (canRunDetection(bedrockPlayer)) {
                            IrregularMovementsBouncing.run(p, to, from,
                                    vertical, oldVertical, box,
                                    knownBoxes, offGround, airTicksOffGround, heightExistsHalf, canGoUp, canGoDown, jumping, falling,
                                    airTicks,
                                    prevention, scenario);
                        }
                        break;
                    case "inside-blocks":
                        if (!bedrockPlayer) {
                            IrregularMovementsInsideBlocks.run(p, to, from,
                                    knownBoxes,
                                    prevention);
                        }
                        break;
                    default:
                        if (!velocityScenario && !waterElevator) {
                            IrregularMovementsLiquids.run(p, to, from,
                                    vertical, horizontal,
                                    pastFalling, !scenario.equals(liquidsKey), knownBoxes, building, jumping, oldJumping, changedScenario,
                                    liquidsKey, prevention);
                        }
                        break;
                }
            }

            boolean airTicksOffGroundCopy = airTicksOffGround,
                    groundInteractionScenarioCopy = groundInteractionScenario,
                    velocityScenarioCopy = velocityScenario;

            if (vertical > minimumUp) {
                if (canGoUp) {
                    if ((!constructDetection || checkUp) && !jumping && !forcefulUpExtended
                            && IrregularMovements.check.getCheck().getBooleanOption("check_upwards", true)) {
                        AsyncDetection.run(p, check, () -> {
                            int buffer = explosion ? 5 :
                                    bedrockPlayer ? 3 :
                                            Math.max(3 - AlgebraUtils.integerFloor(airTicks / ((double) fullOffGroundTicks)), 2);

                            if (!knownBoxes || vertical != box && vertical != oldBox) {
                                if (offGround && !cachedJumpEffect) { // Make sure the player is off the ground to ignore collisions that air-ticks cannot account for
                                    if (airTicksOffGroundCopy) {
                                        cancel(p, from, prevention, "air-up", null, buffer, -defaultBufferTime);
                                    } else if (!bedrockPlayer
                                            || airTicks > MoveUtils.jumpingMotions
                                            || p.getBlocksOffGround(true, true) >= 2) {
                                        boolean suspectedOrHacker = p.getProfile().isSuspectedOrHacker(check)
                                                || check.getCheck().getViolations(p).getLastViolation(true) <= Check.violationCycleSeconds;

                                        if (!velocityScenarioCopy && !groundInteractionScenarioCopy && !incompatibility

                                                && (suspectedOrHacker
                                                || horizontal > 0.0)

                                                && (!oldJumping
                                                || Math.abs(oldVertical - MoveUtils.jumping[0]) >= MoveUtils.jumping[2]) // Continue only when player was not previously jumping and ignore checking them if the jump motion just ended

                                                && !PatternUtils.isBlockPattern(genericHitbox, to, true, BlockUtils.semi_solid)
                                                && !PatternUtils.isBlockPattern(genericHitbox, to, true, BlockUtils.honey_block)) {
                                            cancel(p, from, prevention, "ground-up",
                                                    blockedJumping ? 12 : !changedScenario && (suspectedOrHacker || horizontal < MovementProcessing.sprintingMaxSpeed) ? 4 : 8);
                                        }
                                    }
                                }
                            } else if (airTicksOffGroundCopy) {
                                cancel(p, from, prevention, "box-up", null, buffer, -defaultBufferTime);
                            }
                        });
                    }
                } else if (vertical >= allowedUp && cancel(p, from, prevention, "illegal-up", velocityScenario ? 6 : 3)) {
                    return;
                }

                // Separator

                if (jumping) {
                    if (canJump) {
                        if ((!constructDetection || checkJump) && IrregularMovements.check.getCheck().getBooleanOption("check_jumping", true) && !badTPS) {
                            boolean defaultScenarioCopy = defaultScenario;

                            AsyncDetection.run(p, check, () -> {
                                if (oldJumping) {
                                    if (vertical > oldVertical) {
                                        if (!velocityScenarioCopy && !groundInteractionScenarioCopy) {
                                            cancel(p, from, prevention, greaterJumpDetection, 1);
                                        }
                                    } else if (oldVertical == vertical) {
                                        if (cancel(p, from, prevention, identicalJumpDetection, 1)) {
                                            return;
                                        }
                                    } else if (!cachedJumpEffect && (oldVertical - vertical) >= (MoveUtils.jumping[0] + MoveUtils.lowPrecision)) {
                                        if (!groundInteractionScenarioCopy) {
                                            cancel(p, from, prevention, distantJumpDetection, 1);
                                        }
                                    }
                                } else if (!cachedJumpEffect) {
                                    if (!knownBoxes) {
                                        if (!building && Math.abs(vertical - MoveUtils.jumping[1]) < MoveUtils.jumping[2]) {
                                            cancel(p, from, prevention, unknownJumpDetection, null, 2, -defaultBufferTime);
                                        }
                                    } else if (airTicksOffGroundCopy) { // Don't remove else statement
                                        cancel(p, from, prevention, airJumpDetection, null,
                                                p.getBlocksOffGround(2, true, true) > 1 ? 1 : 2, -defaultBufferTime);
                                    }
                                }

                                if (!knownBoxes && p.isOnGround()
                                        //&& (!p.isOnGroundCustom() || airTicks > 0) // Since first jump motion is not targeted, the player is already expected to be off ground
                                        && jumping
                                        && Math.abs(vertical - MoveUtils.jumping[1]) >= MoveUtils.jumping[2]) { // Do not target the initial jump motion
                                    cancel(p, from, prevention, spoofJumpDetection, 1);
                                } else if (defaultScenarioCopy && p.getLastJump() == 0L && p.getLastFall() >= 2_000L) { // A player must fall to jump and so forth. Not doing so suspects them
                                    cancel(p, from, prevention, uncompletedJumpDetection, 3);
                                }
                            });
                        }
                    } else if (cancel(p, from, prevention, illegalJumpDetection, velocityScenario || forcefulUpExtended ? 6 : 3)) {
                        return;
                    }
                }
            } else if (vertical < minimumDown) {
                int illegalBufferLimit = velocityScenario || forcefulUpExtended || jumpEffect ? 6 : 3;
                Timer timer = p.getTimer(check);
                double slowThreshold = vertical <= -MoveUtils.nearMaxFallingMotion ? 0.0 :
                        vertical <= -1.0 ? (MoveUtils.highPrecision / Math.round(Math.abs(vertical))) :
                                MoveUtils.falling[1] - (MoveUtils.lowPrecision * Math.max(airTicks / 3.0, 3));
                long repeatedMilliseconds = ((offGroundTicks - 1) * 50L) + 5L;
                boolean abstractScenarioCopy = abstractScenario;

                if (canGoDown) {
                    if ((!constructDetection || checkDown) && !falling && !oldFalling && blockY > MoveUtils.voidDamageHeight
                            && IrregularMovements.check.getCheck().getBooleanOption("check_downwards", true)) {
                        AsyncDetection.run(p, check, () -> {
                            if (airTicksOffGroundCopy && offGround && (!groundInteractionScenarioCopy && !explosion || fullAirTicksOffGround)) {
                                if (!abstractScenarioCopy && !changedScenario && !liquidBlocks) {
                                    IrregularMovementsDownwards.run(p, to, from,
                                            vertical,
                                            airTicks, blockY,
                                            repeatedMilliseconds,
                                            slowFalling, jumpEffect,
                                            timer, buffers,
                                            "down", prevention);
                                }

                                if (vertical > oldVertical && !PlayerData.isOnGround(p, to, 0, -1, 0)) {
                                    if (!knownBoxes && !slowFalling) {
                                        cancel(p, from, prevention, greaterDownDetection, null, 3, -defaultBufferTime);
                                    }
                                } else if (oldVertical == vertical) {
                                    cancel(p, from, prevention, identicalDownDetection, null, 3, -defaultBufferTime);
                                } else {
                                    double difference = oldVertical - vertical,
                                            limit = (MoveUtils.falling[2] + MoveUtils.lowPrecision) * (fullAirTicksOffGround ? 1 : 2);

                                    if (difference > limit) {
                                        cancel(p, from, prevention, distantDownDetection, "limit: " + limit, 3, -defaultBufferTime);
                                    } else if (!slowFalling && slowThreshold > 0.0 && difference <= slowThreshold) {
                                        cancel(p, from, prevention, slowDownDetection, "difference: " + difference + ", threshold: " + slowThreshold, 3, -defaultBufferTime);
                                    }
                                }
                            }
                        });
                    }
                } else if (vertical <= -allowedDown && cancel(p, from, prevention, illegalDownDetection, illegalBufferLimit)) {
                    return;
                }

                // Separator

                if (falling || oldFalling) {
                    if (canFall) {
                        if ((!constructDetection || checkFall) && blockY > MoveUtils.voidDamageHeight
                                && IrregularMovements.check.getCheck().getBooleanOption("check_falling", true)) {
                            AsyncDetection.run(p, check, () -> {
                                if (airTicksOffGroundCopy && offGround && (!groundInteractionScenarioCopy && !explosion || fullAirTicksOffGround)) {
                                    if (!abstractScenarioCopy && !changedScenario && !liquidBlocks) {
                                        IrregularMovementsDownwards.run(p, to, from,
                                                vertical,
                                                airTicks, blockY,
                                                repeatedMilliseconds,
                                                slowFalling, jumpEffect,
                                                timer, buffers,
                                                "fall", prevention);
                                    }

                                    if (vertical > oldVertical && !PlayerData.isOnGround(p, to, 0, -1, 0)) {
                                        if (!slowFalling) {
                                            cancel(p, from, prevention, greaterFallDetection, null, knownBoxes ? 5 : 3, -defaultBufferTime);
                                        }
                                    } else if (oldVertical == vertical) {
                                        cancel(p, from, prevention, identicalFallDetection, null, 2, -defaultBufferTime);
                                    } else {
                                        double difference = oldVertical - vertical;
                                        double limit = (MoveUtils.falling[2] + MoveUtils.lowPrecision) * (fullAirTicksOffGround ? 1 : 2);

                                        if (difference > limit) {
                                            cancel(p, from, prevention, distantFallDetection, "limit: " + limit, 2, -defaultBufferTime);
                                        } else if (!slowFalling && slowThreshold > 0.0 && difference <= slowThreshold) {
                                            cancel(p, from, prevention, slowFallDetection, "difference: " + difference + ", threshold: " + slowThreshold, 3, -defaultBufferTime);
                                        }
                                    }
                                }
                            });
                        }
                    } else if (falling && !bouncingBlocks && cancel(p, from, prevention, illegalFallDetection, illegalBufferLimit)) {
                        return;
                    }
                }
            } else if ((!constructDetection || checkHorizontal) && offGround && !forcefulUpExtended
                    && IrregularMovements.check.getCheck().getBooleanOption("check_horizontal", true)

                    && (!heightExists && fullAirTicksOffGround
                    || (bedrockPlayer ? fullAirTicksOffGround : airTicks > 0) && (!heightExists || horizontal >= 0.3)
                    || !bedrockPlayer && airTicks > 5)) {
                boolean uncertainMovement = incompatibility
                        || velocityScenario
                        || groundInteractionScenario
                        || airTicks < fullOffGroundTicks && horizontal < 0.1;
                int buffer = uncertainMovement ? AlgebraUtils.integerCeil(defaultBufferTime / 3.0) :
                        bedrockPlayer ? AlgebraUtils.integerCeil(defaultBufferTime / 4.0) :
                                Math.max((heightExists ? 5 : 3) - (oldFalling ? 0 : AlgebraUtils.integerFloor(airTicks / ((double) fullOffGroundTicks))), 2);

                if (cancel(p, from, prevention, horizontalMotionDetection, null, buffer, -defaultBufferTime)) {
                    return;
                }
            }

            // Separator

            if (!changedScenario && (canGoUp && (!constructDetection || checkUp)
                    || canJump && (!constructDetection || checkJump))) {
                if (!threwTrident) {
                    IrregularMovementsStep.run(p, to, previousFrom,
                            vertical,
                            velocityScenario, damageScenario, groundInteractionScenario, knownBoxes, bedrockPlayer, falling, oldFalling,
                            prevention);
                } else {
                    IrregularMovementsStep.remove(p);
                }
                if (defaultScenario && !cachedJumpEffect && !liquidBlocks && !wasInLiquids && !bedrockPlayer && !waterElevator) {
                    IrregularMovementsHop.run(p, to, from, p.getCooldowns(check), p.getTimer(check),
                            vertical, oldVertical, box, oldBox, horizontal,
                            bedrockPlayer,
                            jumping,
                            prevention);
                }
            } else {
                IrregularMovementsStep.remove(p); // attention: added recently
            }

            // Separator
        } else {
            remove(p);
        }
    }

    static boolean cacheBoolean(Cooldowns cooldowns, boolean bool, String key, int ticks) {
        boolean result;

        if (bool) {
            result = true;
            cooldowns.add(key, ticks);
        } else {
            result = !cooldowns.canDo(key);
        }
        return result;
    }

    static boolean cancel(SpartanPlayer p, SpartanLocation from, String information, String instance, String extra, int buffer, int time) {
        boolean prevention, violate, hasBuffer;
        int bufferAmount;

        if (buffer > 1) {
            hasBuffer = true;
            boolean suspected;

            if (time >= 0) { // Make buffer dynamic when time is equal or above zero
                suspected = p.getProfile().isSuspectedOrHacker(check)
                        || !Compatibility.CompatibilityType.MajorIncompatibility.isFunctional()
                        && !Compatibility.CompatibilityType.ItemAttributes.isFunctional();

                if (suspected && buffer > 2) { // (buffer > 2) added recently to delete single buffers that produce instabilities
                    buffer -= 1; // Decrease buffer limit if player is suspected and if buffer is 3 or more
                }
            } else { // Make buffer static when time is below zero, and return time to absolute state
                time = -time;
                suspected = false;
            }

            if (buffer == 1) { // Skip buffer counting if buffer limit is one
                bufferAmount = 1;
                prevention = true;
                violate = true;
            } else {
                Buffer buffers = p.getBuffer(check);
                String key = instance;
                String minuteKey = key + "=minute";
                violate = (bufferAmount = buffers.start(key, time)) >= buffer;

                if (violate) {
                    prevention = true;
                    buffers.remove(new String[]{key, minuteKey}); // Reset buffer after its completion
                } else {
                    double multiplier = (minuteTicks / ((double) time)) / 5.0;
                    int extendedBuffer = AlgebraUtils.integerRound(buffer * multiplier);

                    if (buffers.start(minuteKey, minuteTicks) >= extendedBuffer) { // Run minute buffer
                        buffer = extendedBuffer;
                        time = minuteTicks;
                        prevention = true;
                        buffers.remove(new String[]{key, minuteKey}); // Reset buffer after its completion
                    } else {
                        prevention = suspected && bufferAmount > 2; // Create fake violation if circumstances are met
                    }
                }
            }
        } else {
            bufferAmount = 0;
            hasBuffer = false;
            prevention = true;
            violate = true;
        }

        if (prevention) {
            boolean groundTeleport = violate && !instance.contains(levitationKey) && !instance.contains(waterElevatorKey);

            HackPreventionUtils.preventVelocity(p, 5);
            new HackPrevention(p,
                    check,
                    "instance: " + instance.split("=")[0] + "(" + buffer + (hasBuffer ? "/" + time + "/" + bufferAmount : "") + ")" + (extra == null ? ", " : ", extra: (" + extra + "), ") + information,
                    violate ? from : null,
                    0,
                    groundTeleport,
                    0.0,
                    violate);
            return violate;
        }
        return false;
    }

    static boolean cancel(SpartanPlayer p, SpartanLocation from, String information, String instance, int buffer) {
        return cancel(p, from, information, instance, null, buffer, defaultBufferTime);
    }

    private static boolean canDo(SpartanPlayer p, SpartanLocation to, boolean bedrockPlayer) {
        return p.canRunCheck(check, true)
                && !p.getHandlers().has(Handlers.HandlerType.ElytraUse) && !p.getHandlers().has(Handlers.HandlerType.Vehicle)
                && (!bedrockPlayer || to.getBlockY() > BlockUtils.getMinHeight(p.getWorld()));
    }

    private static boolean canRunDetection(boolean bedrockPlayer) {
        return !SpartanEdition.freeAlternativeVersion || !bedrockPlayer;
    }
}