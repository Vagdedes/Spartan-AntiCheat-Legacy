package com.vagdedes.spartan.checks.combat;

import com.vagdedes.spartan.abstraction.CheckExecutor;
import com.vagdedes.spartan.checks.movement.speed.Speed;
import com.vagdedes.spartan.compatibility.manual.damage.NoHitDelay;
import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.objects.data.Buffer;
import com.vagdedes.spartan.objects.replicates.SpartanLocation;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerData;
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
                || PlayerData.getPotionLevel(player, PotionEffectType.SPEED) > 0
                && player.getValueOrDefault(player.getNmsHorizontalDistance(), 0.0) >= Speed.ground) {
            preferred = 4.25;
        } else {
            Double y = player.getNmsVerticalDistance();
            preferred = y != null && (player.isJumping(y) || player.isFalling(y)) ? 4.25 : defaultHorizontalReach;
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
                    double distance = AlgebraUtils.getVerticalDistance(player.getEventToLocation(), entityLocation),
                            limit = filter(hackType.getCheck().getDecimalOption("vertical_distance", 4.6), 4.6, 0.0);

                    if (distance >= limit && distance <= CombatUtils.maxHitDistance && !BlockUtils.canClimb(entityLocation)) {
                        cancel("type: vertical, distance: " + distance + ", limit: " + limit);
                    } else {
                        distance = ((double[]) objects[1])[2];
                        limit = filter(hackType.getCheck().getDecimalOption("horizontal_distance", 3.1), defaultHorizontalReach, 0.5);

                        if (distance >= limit) {
                            Double nmsDistance = player.getNmsDistance();
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
                                    && buffer.start("horizontal-short=" + minimumNotReached, ticks) >= attempts) {
                                buffer.remove("horizontal-long");
                                cancel("type: horizontal(short), distance: " + distance + ", limit: " + limit + ", preferred-limit: " + preferredLimit);
                            } else if (buffer.start("horizontal-long", ticks * 2) >= (attempts * 2)) {
                                buffer.clear("horizontal-short");
                                cancel("type: horizontal(long), distance: " + distance + ", limit: " + limit + ", preferred-limit: " + preferredLimit);
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
        return player.canRunCheck(hackType, true)
                && !player.wasFlying()
                && !NoHitDelay.hasCooldown(player);
    }
}
