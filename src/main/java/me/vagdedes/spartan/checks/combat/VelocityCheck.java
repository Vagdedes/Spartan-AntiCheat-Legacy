package me.vagdedes.spartan.checks.combat;

import me.vagdedes.spartan.abstraction.CheckExecutor;
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
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.CombatUtils;
import me.vagdedes.spartan.utils.gameplay.PatternUtils;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class VelocityCheck extends CheckExecutor {

    private static final Enums.HackType check = Enums.HackType.Velocity;

    public static final byte
            REFRESH = 0,
            CAN_DO = 1;

    public VelocityCheck(SpartanPlayer player) {
        super(check, player);
    }

    private PlayerVelocity entityVelocity = null;
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

    @Override
    public boolean run() {
        return false;
    }

    private boolean canDo(boolean local) {
        if ((!local || player.canRunCheck(hackType, true))
                && !player.isUsingItem()
                && !player.isDead()
                && !player.isInTheBorder()
                && !player.wasFlying()
                && !player.hasShield()
                && !player.wasInLiquids()
                && player.getBlocksOffGround(3, true, true) <= 2
                && !player.getHandlers().has(Handlers.HandlerType.Vehicle)
                && (!local || CombatUtils.getEnemiesNumber(player, CombatUtils.maxHitDistance, false, 6) <= 5 && !NoHitDelay.hasCooldown(player))
                && !Essentials.isAFK(player)
                && !Attributes.has(player, Attributes.GENERIC_KNOCKBACK_RESISTANCE)) {
            SpartanLocation loc = player.getLocation();

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

    @Override
    protected boolean canDo() {
        return canDo(true);
    }

    @Override
    public boolean handle(Object object) {
        if (object instanceof LinkedList[]) {
            if (canDo()) {
                PlayerProfile profile = player.getProfile();
                LinkedList<Float> verticalCollection = (LinkedList<Float>) ((LinkedList[]) object)[0],
                        horizontalCollection = (LinkedList<Float>) ((LinkedList[]) object)[1];
                int verticalCount = verticalCollection.size(),
                        validCount = 0,
                        zeroCount = 0;
                boolean verticalCountRequirement = verticalCount >= PlayerVelocity.minimumCollection,
                        checkZeroDistance = hackType.getCheck().getBooleanOption("check_zero_distance", true),
                        suspectedOrHacker = profile.isSuspectedOrHacker(hackType);
                double overallPercentage = 0.0;

                // Separator
                for (int i = 0; i < 2; i++) {
                    boolean horizontal = i == 1;

                    if (horizontal ? PlayerVelocity.canCalculateHorizontal(player) : PlayerVelocity.canCalculateVertical(player)) {
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
                                    Buffer buffer = player.getBuffer(hackType);
                                    String detection = horizontal ? "horizontal" : "vertical";

                                    if (buffer.start(Buffer.BufferType.Combat, detection, ticks) >= (suspectedOrHacker ? hackerFails : fails)) {
                                        buffer.remove(detection);
                                        cancel("type: " + detection
                                                        + ", result: " + overallPercentage
                                                        + ", count: " + count
                                                        + ", valid-count: " + validCount
                                                        + ", fights: " + profile.getCombat().getPastFights().size()
                                                        + ", motion: " + Arrays.toString(collection.toArray(new Float[0])),
                                                null, 20);
                                        return false;
                                    }
                                }
                            }
                        }
                    }
                }

                // Separator
                if (checkZeroDistance && verticalCountRequirement && zeroCount == verticalCount) {
                    Buffer buffer = player.getBuffer(hackType);
                    String key = "zero";

                    if (buffer.start(Buffer.BufferType.Combat, key, ticks) >= ((profile.isSuspectedOrHacker(hackType) ? hackerFails : fails) + 1)) {
                        buffer.remove(key);
                        overallPercentage = (zeroCount / ((double) verticalCount)) * 100.0;
                        cancel(
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
        } else if (object instanceof SpartanLocation) {
            if (entityVelocity != null) {
                entityVelocity.collect(player, (SpartanLocation) object);
            }
        } else if (object.equals(REFRESH)) {
            if (entityVelocity != null) {
                if (entityVelocity.getTimeAllowance() <= System.currentTimeMillis()) {
                    entityVelocity.allowCollection(player, PlayerOpponent.getEntities(player));
                }
            } else {
                entityVelocity = new PlayerVelocity(true);
                entityVelocity.allowCollection(player, PlayerOpponent.getEntities(player)); // Always last because of millisecond counter in the object
            }
        } else {
            return canDo(false);
        }
        return false;
    }
}
