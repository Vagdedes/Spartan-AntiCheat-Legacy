package com.vagdedes.spartan.abstraction.check.implementation.combat;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.implementation.movement.speed.Speed;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.damage.NoHitDelay;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class HitReach extends CheckExecutor {

    public HitReach(SpartanPlayer player) {
        super(HackType.HitReach, player);
    }

    private static final double defaultHorizontalReach = CombatUtils.maxLegitimateHitDistance + 0.1;

    private double getPreferredLimit(LivingEntity entity) {
        double preferred;

        if (entity != null
                && CombatUtils.getWidthAndHeight(entity)[1] > 2.0) {
            preferred = 5.0;
        } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_7)
                || player.hasPotionEffect(PotionEffectType.SPEED, PlayerUtils.getPotionEffectExtraTime(PotionEffectType.SPEED))
                && player.movement.getValueOrDefault(player.movement.getNmsHorizontalDistance(), 0.0) >= Speed.ground) {
            preferred = 4.25;
        } else {
            Double y = player.movement.getNmsVerticalDistance();
            preferred = y != null && (player.movement.isJumping(y) || player.movement.isFalling(y)) ? 4.25 : defaultHorizontalReach;
        }
        return preferred;
    }

    private double filter(double distance, double minimum, double extra) {
        return Math.max(Math.min(CombatUtils.maxHitDistance, distance + extra), minimum);
    }

    @Override
    public boolean runInternal(boolean cancelled) {
        return false;
    }

    @Override
    public boolean handleInternal(boolean cancelled, Object object) {
        if (canDo()) {
            Object[] objects = (Object[]) object;
            LivingEntity entity = (LivingEntity) objects[0];

            if (!CombatUtils.hasBlockBehind(player, entity)) {
                GameMode gm = player.getGameMode();

                if (gm == GameMode.ADVENTURE
                        || gm == GameMode.SURVIVAL) {
                    SpartanPlayer damaged = entity instanceof Player ? SpartanBukkit.getPlayer((Player) entity) : null;
                    boolean isPlayer = damaged != null;
                    SpartanLocation entityLocation = new SpartanLocation(null, entity.getLocation());
                    double distance = AlgebraUtils.getVerticalDistance(player.movement.getEventToLocation(), entityLocation),
                            limit = filter(hackType.getCheck().getDecimalOption("vertical_distance", 4.6), 4.6, 0.0);

                    if (distance >= limit && distance <= CombatUtils.maxHitDistance && !BlockUtils.canClimb(entityLocation)) {
                        cancel("type: vertical, distance: " + distance + ", limit: " + limit);
                    } else {
                        distance = ((double[]) objects[1])[2];
                        limit = filter(hackType.getCheck().getDecimalOption("horizontal_distance", 3.1), defaultHorizontalReach, 0.5);

                        if (distance >= limit) {
                            Double nmsDistance = player.movement.getNmsDistance();
                            double preferredLimit = getPreferredLimit(entity);
                            boolean minimumNotReached = distance < preferredLimit;
                            int ticks = 10 * 11,
                                    attempts = minimumNotReached ? 6
                                            : player.getProfile().isSuspectedOrHacker(hackType)
                                            || player.hasAttackCooldown()
                                            || nmsDistance != null && nmsDistance <= 0.1
                                            || !player.getProfile().playerCombat.isActivelyFighting(damaged, true, true, false) ? 3 : 4;
                            Buffer buffer = player.getBuffer(hackType);

                            if (minimumNotReached && limit < 4.0) {
                                limit = 4.0;
                            }

                            if (!NoHitDelay.hasCooldown(player) && (!isPlayer || !NoHitDelay.hasCooldown(damaged))
                                    && buffer.start("horizontal=" + minimumNotReached, ticks, Check.detectionMeasurementTicks) >= (attempts / (double) ticks)) {
                                cancel("type: horizontal, distance: " + distance + ", limit: " + limit + ", preferred-limit: " + preferredLimit);
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected boolean canDo() {
        return !player.bedrockPlayer
                && player.canRunCheck(hackType, true)
                && !player.movement.isFlying()
                && !player.getTrackers().has(Trackers.TrackerType.GameMode)
                && !NoHitDelay.hasCooldown(player);
    }
}
