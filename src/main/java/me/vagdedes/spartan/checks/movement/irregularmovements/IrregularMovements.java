package me.vagdedes.spartan.checks.movement.irregularmovements;

import me.vagdedes.spartan.compatibility.manual.damage.NoHitDelay;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.features.protections.Building;
import me.vagdedes.spartan.features.protections.Explosion;
import me.vagdedes.spartan.features.synchronicity.SpartanEdition;
import me.vagdedes.spartan.handlers.identifiers.complex.predictable.BouncingBlocks;
import me.vagdedes.spartan.handlers.identifiers.complex.predictable.FloorProtection;
import me.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Damage;
import me.vagdedes.spartan.handlers.identifiers.complex.unpredictable.FishingHook;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.data.Cooldowns;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.hackPrevention.HackPrevention;
import me.vagdedes.spartan.objects.system.hackPrevention.HackPreventionUtils;
import me.vagdedes.spartan.system.Cache;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.utils.gameplay.*;
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

    // Patterns
    private static final double[][]
            interactiveAndPassable = new double[][]{
            {0, 0, 0},
            {0, 1, 0},
    }, liquid = new double[][]{
            {0, 0, 0},
            {0, 1, 0},
    };

    // Detections
    static final int
            defaultBufferTime = 20,
            fullOffGroundTicks = 20;
    private static final int offGroundTicks = 12;

    // Math
    static final int desiredDecimalAccuracy = (8 + 16) / 2;

    public static void remove(SpartanPlayer p) {
        oldFrom.remove(p.getUniqueId());
    }

    public static void refresh(SpartanPlayer p, SpartanLocation loc) {
        oldFrom.put(p.getUniqueId(), loc);
    }

    // Re-enable Fly & IrregularMovements checks
    public static void run(SpartanPlayer p, SpartanLocation to, SpartanLocation from,
                           double vertical, double oldVertical, double verticalDifference, double oldVerticalDifference,
                           double horizontal, double box, double oldBox,
                           int airTicks) {
        boolean bedrockPlayer = p.isBedrockPlayer();

        if (canDo(p, to, bedrockPlayer)) {
            // Scenario Management
            double allowedUp = 0.0, allowedDown = 0.0;
            boolean canGoUp, canGoDown,
                    canJump, canFall,
                    groundInteractionScenario = false, // Real
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
            boolean levitationEffect = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && PlayerData.hasLevitationEffect(p),
                    incompatibility = Compatibility.CompatibilityType.MajorIncompatibility.isFunctional() || Compatibility.CompatibilityType.ItemAttributes.isFunctional(),
                    fullAirTicksOffGround = airTicks >= fullOffGroundTicks,
                    airTicksOffGround = fullAirTicksOffGround || airTicks >= (bedrockPlayer || incompatibility ? fullOffGroundTicks : offGroundTicks),
                    onGround = p.isOnGround(),
                    onGroundCustom = p.isOnGroundCustom(),
                    offGround = !onGroundCustom || !onGround && !liquidBlocks,
                    knownBoxesNew = GroundUtils.heightExists(box, true),
                    knownBoxesOld = GroundUtils.heightExists(oldBox, true),
                    knownBoxes = knownBoxesNew || knownBoxesOld,
                    bouncingBlocks = (!knownBoxes || airTicks > 0) && BouncingBlocks.isBelow(p),
                    jumpEffect = p.hasPotionEffect(PotionEffectType.JUMP),
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
            boolean heightExists = cacheBoolean(cooldowns, knownBoxes, "height-exists=half", 10),
                    building = cacheBoolean(cooldowns, Building.hasCooldown(p) || p.getHandlers().has(Handlers.HandlerType.BlockPlace), "building", 20),
                    pastFalling = cacheBoolean(cooldowns, falling || oldFalling, "past-falling", 20);

            // Combination Variables
            boolean forcefulUp = levitationEffect || waterElevator || Explosion.justExploded(p),
                    forcefulUpExtended = forcefulUp || threwTrident; // Used to prevent false positives and not heavily modify detections

            // Scenario Identification
            if (threwTrident
                    || PlayerData.hasNearbyCollisions(p)
                    || NoHitDelay.hasCooldown(p)
                    || p.getHandlers().has(Handlers.HandlerType.Piston)
                    || FishingHook.hasCooldown(p)) { // Extreme circumstances
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
                    minimumDown = -(MoveUtils.gravityAcceleration * 7);
                } else {
                    scenario = liquidsKey;
                    canJump = true;
                    constructDetection = true;
                }
                canGoUp = true;
                canGoDown = !forcefulUp;
                canFall = !forcefulUp;
            } else if (explosion || Damage.hasCooldown(p)) { // Damage Or Combat
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
                    {BlockUtils.hitbox_max, -1, BlockUtils.hitbox_max, onGround || onGroundCustom ? 1 : 0}
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
            boolean changedScenario = cacheBoolean(cooldowns, buffers.get(changeScenario, scenarioCode) != scenarioCode, "changed-scenario", 7);

            // Variable Modification
            if (waterElevator) { // Allow the player to go a bit down when in a water soul-sand elevator
                allowedDown = MoveUtils.gravityAcceleration * 6;
            } else if (forcefulUp) { // Allow the player to go a bit down when in an upwards-only motion
                allowedDown = MoveUtils.gravityAcceleration * 4;
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
                                    knownBoxes, offGround, airTicksOffGround, heightExists, canGoUp, canGoDown, jumping, falling,
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

            // Has important counters, so put it first regardless
            IrregularMovementsMotion.run(p,
                    to, from,
                    vertical, oldVertical, verticalDifference, oldVerticalDifference,
                    box, oldBox,
                    knownBoxes, knownBoxesNew, knownBoxesOld,
                    onGround, onGroundCustom,
                    constructDetection, groundInteractionScenario, velocityScenario,
                    canGoUp && checkUp, canGoDown && checkDown,
                    canJump && checkJump, canFall && checkFall, checkHorizontal,
                    airTicks,
                    prevention);

            if (vertical > minimumUp) {
                if (!canGoUp
                        && vertical >= allowedUp
                        && IrregularMovements.check.getCheck().getBooleanOption("check_upwards", true)
                        && cancel(p, from, prevention, "illegal-up", velocityScenario ? 6 : 3)) {
                    return;
                }
                if (jumping
                        && !canJump
                        && IrregularMovements.check.getCheck().getBooleanOption("check_jumping", true)) {
                    cancel(p, from, prevention, "illegal-jump", velocityScenario || forcefulUpExtended ? 6 : 3);
                }
            } else if (vertical < minimumDown) {
                int illegalBufferLimit = velocityScenario || forcefulUpExtended || jumpEffect ? 6 : 3;

                if (falling || oldFalling) {
                    if (!canFall
                            && !bouncingBlocks
                            && IrregularMovements.check.getCheck().getBooleanOption("check_fall", true)) {
                        cancel(p, from, prevention, "illegal-fall", illegalBufferLimit);
                    }
                } else if (!canGoDown
                        && vertical <= -allowedDown
                        && IrregularMovements.check.getCheck().getBooleanOption("check_downwards", true)) {
                    cancel(p, from, prevention, "illegal-down", illegalBufferLimit);
                }
            }
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

    static void clear(SpartanPlayer p, String instance) {
        boolean sharedInstance = instance.charAt(0) == '*';
        p.getBuffer(IrregularMovements.check).remove(sharedInstance ? instance.split("-", 2)[0].substring(1) : instance);
    }

    static boolean cancel(SpartanPlayer p, SpartanLocation from, String information, String instance, String extra, int buffer, int time) {
        boolean prevention, violate, hasBuffer,
                sharedInstance = instance.charAt(0) == '*';
        int bufferAmount;

        if (buffer > 1) {
            boolean suspected;

            if (time >= 0) { // Make buffer dynamic when time is equal or above zero
                suspected = p.getProfile().isSuspectedOrHacker(check)
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

            Buffer bufferObj = p.getBuffer(check);
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
            HackPreventionUtils.preventVelocity(p, 5);
            instance = sharedInstance ? instance.substring(1) : instance.split("=", 2)[0]; // Remove potential variables and special characters
            new HackPrevention(p,
                    check,
                    "instance: " + instance + "(" + buffer + (hasBuffer ? "/" + time + "/" + bufferAmount : "") + "), "
                            + information + (extra == null ? "" : ", extra: 1, " + extra),
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