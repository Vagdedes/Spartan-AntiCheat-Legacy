package me.vagdedes.spartan.checks.implementation.combat;

import me.vagdedes.spartan.compatibility.manual.damage.NoHitDelay;
import me.vagdedes.spartan.compatibility.manual.essential.Essentials;
import me.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.profiling.PlayerOpponent;
import me.vagdedes.spartan.objects.profiling.PlayerProfile;
import me.vagdedes.spartan.objects.profiling.PlayerVelocity;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.HackPrevention;
import me.vagdedes.spartan.system.Cache;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.CombatUtils;
import me.vagdedes.spartan.utils.gameplay.PatternUtils;

import java.util.*;

public class VelocityCheck {

    private static final Enums.HackType check = Enums.HackType.Velocity;
    private static final Map<UUID, PlayerVelocity> entityMemory = Cache.store(new LinkedHashMap<>());
    private static final int
            ticks = 100,
            fails = 4,
            hackerFails = (fails - 1);

    private static final double[][]
            solid = new double[][]{
            {0, 1, 0},
            {0, 2, 0}
    }, web = new double[][]{
            {BlockUtils.hitbox_max, -1, BlockUtils.hitbox_max},
            {BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max}
    }, semiSolid = new double[][]{
            {BlockUtils.hitbox_max, -1, BlockUtils.hitbox_max},
            {BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max},
            {BlockUtils.hitbox_max, 1, BlockUtils.hitbox_max},
            {BlockUtils.hitbox_max, 2, BlockUtils.hitbox_max}
    };

    public static void remove(SpartanPlayer player) {
        entityMemory.remove(player.getUniqueId());
    }

    public static void collectEntity(SpartanPlayer player, SpartanLocation location) {
        PlayerVelocity velocity = entityMemory.get(player.getUniqueId());

        if (velocity != null) {
            velocity.collect(player, location);
        }
    }

    public static void refreshEntity(SpartanPlayer player) {
        UUID uuid = player.getUniqueId();
        PlayerVelocity velocity = entityMemory.get(uuid);

        if (velocity != null) {
            if (velocity.getTimeAllowance() <= System.currentTimeMillis()) {
                velocity.allowCollection(player, PlayerOpponent.getEntities(player));
            }
        } else {
            velocity = new PlayerVelocity(true);
            entityMemory.put(uuid, velocity);
            velocity.allowCollection(player, PlayerOpponent.getEntities(player)); // Always last because of millisecond counter in the object
        }
    }

    public static void run(SpartanPlayer p, LinkedList<Float> verticalCollection, LinkedList<Float> horizontalCollection) {
        if (canDo(p, true)) {
            PlayerProfile profile = p.getProfile();
            int verticalCount = verticalCollection.size(),
                    validCount = 0,
                    zeroCount = 0;
            boolean verticalCountRequirement = verticalCount >= PlayerVelocity.minimumCollection,
                    checkZeroDistance = check.getCheck().getBooleanOption("check_zero_distance", true),
                    suspectedOrHacker = profile.isSuspectedOrHacker(check);
            double overallPercentage = 0.0;

            // Separator
            for (int i = 0; i < 2; i++) {
                boolean horizontal = i == 1;

                if (horizontal ? PlayerVelocity.canCalculateHorizontal(p) : PlayerVelocity.canCalculateVertical(p)) {
                    List<Float> collection = horizontal ? horizontalCollection : verticalCollection;
                    int count = horizontal ? collection.size() : verticalCount;

                    if (horizontal ? count >= PlayerVelocity.minimumCollection : verticalCountRequirement) {
                        for (float value : collection) {
                            if (!horizontal && checkZeroDistance && value == 0.0) { // Count only vertical movements for the zero detection
                                zeroCount++;
                            }
                            double percentage = ResearchEngine.getGlobalVelocityOccurrences(value, count, !horizontal);

                            if (percentage != -1.0) {
                                validCount++;
                                overallPercentage += percentage;
                            }
                        }

                        if (validCount > (count / 2)) { // Continue only if more than half of the count is valid
                            overallPercentage /= validCount;

                            if (overallPercentage < 1.0) {
                                Buffer buffer = p.getBuffer(check);
                                String detection = horizontal ? "horizontal" : "vertical";
                                String key = detection;

                                if (buffer.start(Buffer.BufferType.Combat, key, ticks) >= (suspectedOrHacker ? hackerFails : fails)) {
                                    buffer.remove(key);
                                    new HackPrevention(p, check, "type: " + detection
                                            + ", result: " + overallPercentage
                                            + ", count: " + count
                                            + ", valid-count: " + validCount
                                            + ", fights: " + profile.getCombat().getPastFights().size()
                                            + ", motion: " + Arrays.toString(collection.toArray(new Float[0])),
                                            null, 20);
                                    return;
                                }
                            }
                        }
                    }
                }
            }

            // Separator
            if (checkZeroDistance && verticalCountRequirement && zeroCount == verticalCount) {
                Buffer buffer = p.getBuffer(check);
                String key = "zero";

                if (buffer.start(Buffer.BufferType.Combat, key, ticks) >= ((profile.isSuspectedOrHacker(check) ? hackerFails : fails) + 1)) {
                    buffer.remove(key);
                    overallPercentage = (zeroCount / ((double) verticalCount)) * 100.0;
                    new HackPrevention(p, check,
                            "type: zero"
                                    + ", result: " + overallPercentage
                                    + ", count: " + verticalCount
                                    + ", zero-count: " + zeroCount
                                    + ", fights: " + profile.getCombat().getPastFights().size()
                                    + ", motion: " + Arrays.toString(verticalCollection.toArray(new Float[0])),
                            null, 20);
                }
            }
        }
    }

    public static boolean canDo(SpartanPlayer p, boolean local) {
        if ((!local || p.canRunCheck(check, true))
                && !p.isUsingItem()
                && !p.isDead()
                && !p.isInTheBorder()
                && !p.wasFlying()
                && !p.hasShield()
                && !p.wasInLiquids()
                && p.getBlocksOffGround(3, true, true) <= 2
                && !p.getHandlers().has(Handlers.HandlerType.Vehicle)
                && (!local || CombatUtils.getEnemiesNumber(p, CombatUtils.maxHitDistance, false, 6) <= 5 && !NoHitDelay.hasCooldown(p))
                && !Essentials.isAFK(p)
                && !Attributes.has(p, Attributes.GENERIC_KNOCKBACK_RESISTANCE)) {
            SpartanLocation loc = p.getLocation();

            if (!BlockUtils.canClimb(loc)) {
                if (local) {
                    return !PatternUtils.isBlockPattern(solid, loc, false, BlockUtils.solid, BlockUtils.semi_solid)
                            && !PatternUtils.isBlockPattern(web, loc, true, BlockUtils.web)
                            && !PatternUtils.isBlockPattern(semiSolid, loc, true, BlockUtils.semi_solid);
                }
                return true;
            }
        }
        return false;
    }
}
