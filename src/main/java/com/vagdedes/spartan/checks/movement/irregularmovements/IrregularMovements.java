package com.vagdedes.spartan.checks.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.data.Cooldowns;
import com.vagdedes.spartan.abstraction.data.Handlers;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.damage.NoHitDelay;
import com.vagdedes.spartan.functionality.identifiers.complex.predictable.BouncingBlocks;
import com.vagdedes.spartan.functionality.identifiers.complex.predictable.Damage;
import com.vagdedes.spartan.functionality.identifiers.simple.DetectionLocation;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import com.vagdedes.spartan.utils.gameplay.PatternUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.potion.PotionEffectType;

import java.util.Objects;

public class IrregularMovements extends CheckExecutor {

    // Base
    public static final HackType check = HackType.IrregularMovements;
    private final IrregularMovementsBouncing bouncing;
    private final IrregularMovementsClimb climb;
    private final IrregularMovementsCobweb cobweb;
    private final IrregularMovementsInsideBlocks insideBlocks;
    private final IrregularMovementsLevitation levitation;
    private final IrregularMovementsLiquids liquids;
    private final IrregularMovementsMotion motion;

    public IrregularMovements(SpartanPlayer player) {
        super(check, player, 7);
        this.addDetections(new DetectionExecutor[]{
                new IrregularMovementsBouncing(this),
                new IrregularMovementsClimb(this),
                new IrregularMovementsCobweb(this),
                new IrregularMovementsInsideBlocks(this),
                new IrregularMovementsLevitation(this),
                new IrregularMovementsLiquids(this),
                new IrregularMovementsMotion(this)
        });
        bouncing = (IrregularMovementsBouncing) detections[0];
        climb = (IrregularMovementsClimb) detections[1];
        cobweb = (IrregularMovementsCobweb) detections[2];
        insideBlocks = (IrregularMovementsInsideBlocks) detections[3];
        levitation = (IrregularMovementsLevitation) detections[4];
        liquids = (IrregularMovementsLiquids) detections[5];
        motion = (IrregularMovementsMotion) detections[6];
    }

    private SpartanLocation oldFrom = null;

    // Keys
    private static final String
            changeScenario = "changed-scenario",
            levitationKey = "levitation",
            waterElevatorKey = "water-elevator";
    static final String liquidsKey = "liquids";

    // Detections
    static final int
            defaultBufferTime = 20,
            fullOffGroundTicks = 20;
    private static final int offGroundTicks = 12;

    @Override
    public boolean runInternal(boolean cancelled) {
        Double vertical = player.movement.getNmsVerticalDistance(),
                oldVertical = player.movement.getPreviousNmsVerticalDistance(),
                horizontal = player.movement.getNmsHorizontalDistance(),
                oldHorizontal = player.movement.getPreviousNmsHorizontalDistance(),
                box = player.movement.getNmsBox(),
                oldBox = player.movement.getPreviousNmsBox();

        if (vertical == null
                || oldVertical == null
                || horizontal == null
                || oldHorizontal == null
                || box == null
                || oldBox == null) {
            return false;
        }
        SpartanLocation to = player.movement.getEventToLocation(),
                from = player.movement.getEventFromLocation();
        int airTicks = player.movement.getTicksOnAir();
        boolean bedrockPlayer = player.bedrockPlayer;

        if (canDo()) {
            // New Variables
            double verticalDifference = vertical - oldVertical,
                    horizontalDifference = horizontal - oldHorizontal;

            // Scenario Management
            double allowedUp = 0.0, allowedDown = 0.0;
            boolean canGoUp, canGoDown,
                    canJump, canFall,
                    groundInteractionScenario = false, climbScenario = false, slowFallingScenario = false, // Real
                    velocityScenario = false; // Combinations
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
                    waterElevator = player.getHandlers().has(Handlers.HandlerType.WaterElevator);

            if (waterElevator
                    || PatternUtils.collides(to, true, true, 0.0, BlockUtils.liquid)) {
                liquidBlocks = true;
            } else if (vertical < 0.0) {
                if (BlockUtils.isLiquid(to.clone().add(0, vertical, 0))) {
                    liquidBlocks = true;
                } else {
                    Boolean result = PatternUtils.getCached(to, hackType, "liquid-blocks");

                    if (result == null) {
                        for (double i = -1.0; i > Math.floor(vertical); i--) {
                            if (BlockUtils.isLiquid(to.clone().add(0, i, 0))) {
                                result = true;
                                break;
                            }
                        }
                        result = PatternUtils.cache(to, hackType, "liquid-blocks", result != null);
                    }
                    liquidBlocks = result;
                }
            } else {
                liquidBlocks = false;
            }

            // Allow check to run when all cache is collected
            if (oldFrom == null) {
                oldFrom = from;
                return false;
            }

            // Final Variables
            Buffer buffers = player.getBuffer(hackType);
            boolean levitationEffect = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && PlayerUtils.hasLevitationEffect(player),
                    incompatibility = Compatibility.CompatibilityType.MajorIncompatibility.isFunctional() || Compatibility.CompatibilityType.ItemAttributes.isFunctional(),
                    fullAirTicksOffGround = airTicks >= fullOffGroundTicks,
                    airTicksOffGround = fullAirTicksOffGround || airTicks >= (bedrockPlayer || incompatibility ? fullOffGroundTicks : offGroundTicks),
                    onGroundCustom = player.isOnGroundCustom(),
                    offGround = !onGroundCustom || !player.isOnGround() && !liquidBlocks,
                    knownBoxesNew = GroundUtils.heightExists(box),
                    knownBoxesOld = GroundUtils.heightExists(oldBox),
                    knownBoxes = knownBoxesNew || knownBoxesOld,
                    bouncingBlocks = BouncingBlocks.isBelow(player),
                    jumpEffect = player.hasPotionEffect(PotionEffectType.JUMP),
                    threwTrident = player.getHandlers().has(Handlers.HandlerType.Trident),
                    explosion = Damage.hasExplosion(player),
                    falling = player.movement.isFalling(vertical),
                    oldFalling = player.movement.isFalling(oldVertical),
                    jumping = player.movement.isJumping(vertical),
                    oldJumping = player.movement.isJumping(oldVertical),
                    wasInLiquids = player.movement.wasInLiquids();

            // Cache Variables
            Cooldowns cooldowns = player.getCooldowns(hackType);
            boolean heightExists = cacheBoolean(cooldowns, knownBoxes, "height-exists=half", 10),
                    building = cacheBoolean(cooldowns,
                            player.getHandlers().has(Handlers.HandlerType.TowerBuilding), "building", 20),
                    pastFalling = cacheBoolean(cooldowns, falling || oldFalling, "past-falling", 20);

            // Combination Variables
            boolean forcefulUp = levitationEffect || waterElevator,
                    forcefulUpExtended = forcefulUp || threwTrident; // Used to prevent false positives and not heavily modify detections

            // Scenario Identification
            if (threwTrident
                    || PlayerUtils.hasNearbyCollisions(player)
                    || NoHitDelay.hasCooldown(player)
                    || player.getHandlers().has(Handlers.HandlerType.Piston)) { // Extreme circumstances
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
                    minimumDown = -(PlayerUtils.gravityAcceleration * 7);
                } else {
                    scenario = liquidsKey;
                    canJump = true;
                    constructDetection = true;
                }
                canGoUp = true;
                canGoDown = !forcefulUp;
                canFall = !forcefulUp;
            } else if (explosion
                    || Damage.isSignificant(player)) { // Damage Or Combat
                velocityScenario = true;

                scenario = "damage/explosion";
                canGoUp = true;
                canGoDown = !forcefulUp;
                canJump = !forcefulUp;
                canFall = !forcefulUp;
            } else if (!player.movement.isCrawling()

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
            } else if (PatternUtils.collides(to, true, true, BlockUtils.hitbox, BlockUtils.web)) { // Cobwebs
                scenario = "cobweb";
                canGoUp = forcefulUp;
                canGoDown = !forcefulUp;
                canJump = false;
                canFall = !forcefulUp;

                constructDetection = true; // all
                minimumUp = 0.022;
            } else if (player.getVehicle() == null
                    && PatternUtils.collides(to, true, true, 0.62, BlockUtils.climbable)) { // Ladder/Vine Climbing
                climbScenario = true;

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
            } else if (player.isOnFire()
                    || player.isFrozen()
                    || player.getLastDamageReceived().isSubtle()
                    || PlayerUtils.hasBadPotionEffects(player)
                    || PatternUtils.collides(to, true, true, 0.0, BlockUtils.interactive_and_passable)) { // Small damage by effects, circumstances or fire
                groundInteractionScenario = true;

                scenario = "ground-interaction";
                canGoUp = true;
                canGoDown = !forcefulUp;
                canJump = !forcefulUp;
                canFall = !forcefulUp;
            } else if (PlayerUtils.hasSlowFallingEffect(player)) { // Slow Falling
                slowFallingScenario = true;

                scenario = "slow-falling";
                canGoUp = true;
                canGoDown = !forcefulUp;
                canJump = !forcefulUp;
                canFall = !forcefulUp;
            } else if (levitationEffect) { // Levitation Effect
                scenario = levitationKey;
                canGoUp = true;
                canGoDown = false;
                canJump = false;
                canFall = false;

                constructDetection = true; // all
            } else { // Default scenario
                scenario = "neutral";
                canGoUp = true;
                canGoDown = true;
                canJump = true;
                canFall = true;
            }
            if (!constructDetection) {
                checkUp = !forcefulUpExtended;
                checkDown = true;
                checkJump = true;
                checkFall = true;
                checkHorizontal = true;
            } else {
                checkUp &= !forcefulUpExtended;
            }
            velocityScenario |= waterElevator;

            // Scenario Tracker
            int scenarioCode = Objects.hash(
                    scenario,
                    canGoUp, canGoDown, canJump, canFall,
                    checkUp, checkDown, checkJump, checkFall, checkHorizontal
            );
            boolean changedScenario = cacheBoolean(cooldowns, buffers.get(changeScenario, scenarioCode) != scenarioCode, "changed-scenario", 5);
            buffers.set(IrregularMovements.changeScenario, scenarioCode);

            // Variable Modification
            if (waterElevator) { // Allow the player to go a bit down when in a water soul-sand elevator
                allowedDown = PlayerUtils.gravityAcceleration * 6;
            } else if (forcefulUp) { // Allow the player to go a bit down when in an upwards-only motion
                allowedDown = PlayerUtils.gravityAcceleration * 4;
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
                    + ", air: " + airTicks + ", motion-acceleration: " + verticalDifference
                    + ", horizontal: " + horizontal + ", horizontal-acceleration: " + horizontalDifference;

            // Do not process if the player is not in a water elevator or water block but has liquid cooldown
            if (!waterElevator && !liquidBlocks && wasInLiquids) {
                if (!velocityScenario) {
                    liquids.run(pastFalling, true, knownBoxes, building, jumping,
                            oldJumping, changedScenario, liquidsKey, prevention);
                }
                return false;
            }

            if (constructDetection) {
                switch (scenario) {
                    case "climb":
                        if (!bedrockPlayer) {
                            climb.run(cooldowns, knownBoxes, canGoUp, canGoDown,
                                    canJump, bouncingBlocks, jumping, prevention, scenario);
                        }
                        break;
                    case levitationKey:
                        if (!waterElevator) {
                            levitation.run(cooldowns, canGoUp, prevention, scenario);
                        }
                        break;
                    case "cobweb":
                        cobweb.run(canGoDown, prevention, scenario);
                        break;
                    case "bouncing":
                        bouncing.run(
                                knownBoxes, offGround, airTicksOffGround, heightExists,
                                canGoUp, canGoDown,
                                jumping, falling,
                                levitationEffect,
                                prevention, scenario);
                        break;
                    case "inside-blocks":
                        if (!bedrockPlayer) {
                            insideBlocks.run(knownBoxes, prevention);
                        }
                        break;
                    default:
                        if (!velocityScenario && !waterElevator) {
                            liquids.run(pastFalling, !scenario.equals(liquidsKey), knownBoxes, building,
                                    jumping, oldJumping, changedScenario, liquidsKey, prevention);
                        }
                        break;
                }
            }

            // Has important counters, so put it first regardless
            motion.run(
                    cooldowns,
                    verticalDifference,
                    horizontalDifference,
                    knownBoxes, knownBoxesNew, knownBoxesOld,
                    offGround, onGroundCustom,
                    oldFalling, levitationEffect,
                    constructDetection, groundInteractionScenario, velocityScenario, changedScenario, climbScenario, slowFallingScenario,
                    canGoUp && checkUp, canGoDown && checkDown,
                    canJump && checkJump, canFall && checkFall, checkHorizontal,
                    prevention);

            if (vertical > minimumUp) {
                if (!canGoUp
                        && vertical >= allowedUp
                        && hackType.getCheck().getBooleanOption("check_upwards", true)
                        && cancel(prevention, "illegal-up", velocityScenario ? 6 : 3)) {
                    return false;
                }
                if (jumping
                        && !canJump
                        && hackType.getCheck().getBooleanOption("check_jumping", true)) {
                    cancel(prevention, "illegal-jump", velocityScenario || forcefulUpExtended ? 6 : 3);
                }
            } else if (vertical < minimumDown) {
                int illegalBufferLimit = velocityScenario || forcefulUpExtended || jumpEffect ? 6 : 3;

                if (falling || oldFalling) {
                    if (!canFall
                            && !bouncingBlocks
                            && hackType.getCheck().getBooleanOption("check_fall", true)) {
                        cancel(prevention, "illegal-fall", illegalBufferLimit);
                    }
                } else if (!canGoDown
                        && vertical <= -allowedDown
                        && hackType.getCheck().getBooleanOption("check_downwards", true)) {
                    cancel(prevention, "illegal-down", illegalBufferLimit);
                }
            }
        } else {
            oldFrom = null;
        }
        return false;
    }

    boolean cacheBoolean(Cooldowns cooldowns, boolean bool, String key, int ticks) {
        if (bool) {
            cooldowns.add(key, ticks);
            return true;
        } else {
            return !cooldowns.canDo(key);
        }
    }

    void forgetBoolean(Cooldowns cooldowns, String key) {
        cooldowns.remove(key);
    }

    void clear(SpartanPlayer p, String instance) {
        boolean sharedInstance = instance.charAt(0) == '*';
        p.getBuffer(hackType).remove(sharedInstance ? instance.split("-", 2)[0].substring(1) : instance);
    }

    boolean cancel(String information, String instance, String extra, int buffer, int time, boolean increaseViolations) {
        boolean prevention, violate, hasBuffer,
                sharedInstance = instance.charAt(0) == '*';
        int bufferAmount;

        if (buffer > 1) {
            boolean suspected;

            if (time >= 0) { // Make buffer dynamic when time is equal or above zero
                suspected = player.getProfile().isSuspectedOrHacker(hackType)
                        || !Compatibility.CompatibilityType.MajorIncompatibility.isFunctional()
                        && !Compatibility.CompatibilityType.ItemAttributes.isFunctional();

                if (suspected) {
                    buffer -= 1; // Decrease buffer limit if player is suspected and if buffer is 2 or more
                    hasBuffer = buffer > 1;
                } else {
                    hasBuffer = true;
                }
            } else { // Make buffer static when time is below zero, and return time to absolute state
                time = -time;
                suspected = false;
                hasBuffer = true;
            }

            Buffer bufferObj = player.getBuffer(hackType);
            String key = sharedInstance ? instance.split("-", 2)[0].substring(1) : instance;
            violate = (bufferAmount = bufferObj.start(key, time)) >= buffer;

            if (violate) {
                prevention = true;
                bufferObj.remove(key); // Reset buffer after its completion
            } else {
                prevention = suspected && bufferAmount > 2; // Create fake violation if circumstances are met
            }
        } else {
            bufferAmount = 0;
            hasBuffer = false;
            prevention = true;
            violate = true;
        }

        if (prevention) {
            boolean groundTeleport = violate && !instance.contains(levitationKey) && !instance.contains(waterElevatorKey);
            instance = sharedInstance ? instance.substring(1) : instance.split("=", 2)[0]; // Remove potential variables and special characters
            cancel(
                    "type: " + instance + "(" + buffer + (hasBuffer ? "/" + time + "/" + bufferAmount : "") + "), "
                            + information + (extra == null ? "" : ", extra: 1, " + extra),
                    violate ? DetectionLocation.get(player, true) : null,
                    0,
                    groundTeleport,
                    0.0,
                    increaseViolations ? 1 : 0
            );
            return violate;
        }
        return false;
    }

    boolean cancel(String information, String instance, String extra, int buffer, int time) {
        return cancel(information, instance, extra, buffer, time, true);
    }

    boolean cancel(String information, String instance, int buffer) {
        return cancel(information, instance, null, buffer, defaultBufferTime);
    }

    @Override
    public boolean handleInternal(boolean cancelled, Object object) {
        if (object instanceof Integer) {
            this.machineLearningDetection(
                    SpartanBukkit.movementPatterns.vertical.getGeneralization(GroundUtils.maxHeightLength),
                    (int) object
            );
        } else {
            if (object == null) {
                oldFrom = null;
            } else {
                oldFrom = (SpartanLocation) object;
            }
        }
        return false;
    }

    @Override
    protected boolean canDo() {
        return player.canRunCheck(hackType, true)
                && !player.getHandlers().has(Handlers.HandlerType.ElytraUse)
                && (!player.bedrockPlayer || player.movement.getEventToLocation().getBlockY() > BlockUtils.getMinHeight(player.getWorld()));
    }
}