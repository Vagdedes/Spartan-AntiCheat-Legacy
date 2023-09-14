package me.vagdedes.spartan.checks.combat;

import me.vagdedes.spartan.checks.movement.speed.Speed;
import me.vagdedes.spartan.compatibility.manual.damage.NoHitDelay;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.data.Cooldowns;
import me.vagdedes.spartan.objects.profiling.PlayerFight;
import me.vagdedes.spartan.objects.profiling.PlayerOpponent;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.HackPrevention;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.CombatUtils;
import me.vagdedes.spartan.utils.gameplay.PlayerData;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class HitReach {

    private static final HackType check = HackType.HitReach;
    private static final double defaultHorizontalReach = CombatUtils.maxLegitimateHitDistance + 0.1;

    public static void run(SpartanPlayer p, SpartanLocation to, LivingEntity entity, double[] utility) {
        if (p.canRunCheck(check, true)
                && !p.wasFlying()
                && !NoHitDelay.hasCooldown(p)
                && !CombatUtils.hasBlockBehind(p, entity)) {
            GameMode gm = p.getGameMode();

            if (gm == GameMode.ADVENTURE || gm == GameMode.SURVIVAL) {
                SpartanPlayer damaged = entity instanceof Player ? SpartanBukkit.getPlayer((Player) entity) : null;
                boolean isPlayer = damaged != null;
                SpartanLocation entityLocation = new SpartanLocation(null, entity.getLocation());
                double distance = AlgebraUtils.getVerticalDistance(to, entityLocation),
                        limit = filter(check.getCheck().getDecimalOption("vertical_distance", 4.6), 4.6, 0.0);

                if (distance >= limit && distance <= CombatUtils.maxHitDistance && !BlockUtils.canClimb(entityLocation)) {
                    new HackPrevention(p, check, "type: vertical, distance: " + distance + ", limit: " + limit);
                } else {
                    distance = utility[2];

                    if (!p.isChasing()) {
                        limit = filter(check.getCheck().getDecimalOption("horizontal_distance", 3.1), defaultHorizontalReach, 0.5);

                        if (distance >= limit) {
                            double preferredLimit = getPreferredLimit(p, entity);
                            boolean minimumNotReached = distance < preferredLimit;
                            int ticks = 10 * 11,
                                    attempts = minimumNotReached ? 6
                                            : p.getProfile().isSuspectedOrHacker(check)
                                            || p.hasAttackCooldown()
                                            || p.getNmsDistance() <= 0.1
                                            || !PlayerData.isInActivePlayerCombat(p) ? 3 : 4;
                            Buffer buffer = p.getBuffer(check);

                            if (minimumNotReached && limit < 4.0) {
                                limit = 4.0;
                            }

                            if (!NoHitDelay.hasCooldown(p) && (!isPlayer || !NoHitDelay.hasCooldown(damaged))
                                    && buffer.start(Buffer.BufferType.Combat, "horizontal-short=" + minimumNotReached, ticks) >= attempts) {
                                buffer.remove("horizontal-long");
                                new HackPrevention(p, check, "type: horizontal(short), distance: " + distance + ", limit: " + limit + ", preferred-limit: " + preferredLimit);
                            } else if (buffer.start(Buffer.BufferType.Combat, "horizontal-long", ticks * 2) >= (attempts * 2)) {
                                buffer.clear("horizontal-short");
                                new HackPrevention(p, check, "type: horizontal(long), distance: " + distance + ", limit: " + limit + ", preferred-limit: " + preferredLimit);
                            }
                        }

                        // Separator
                        if (isPlayer) {
                            String detection = "horizontal(historical)",
                                    key = detection + "=" + entity.getEntityId();
                            Cooldowns cooldowns = p.getCooldowns(check);

                            if (cooldowns.canDo(key)) {
                                PlayerOpponent opponent = p.getProfile().getCombat().getCurrentFight(damaged).getOpponent(p)[0];

                                if (opponent.getHits() >= PlayerFight.requiredHits) {
                                    double reachAverage = opponent.getReachAverage();

                                    if (reachAverage >= limit) {
                                        cooldowns.add(key, CombatUtils.getFightTicks(p) * PlayerFight.requiredHits);
                                        new HackPrevention(p, check, "type: " + detection + ", distance: " + reachAverage + ", limit: " + limit);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static double getPreferredLimit(SpartanPlayer p, LivingEntity entity) {
        double preferred;

        if (entity != null && CombatUtils.getWidthAndHeight(entity)[1] > 2.0) {
            preferred = 5.0;
        } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_7)
                || PlayerData.getPotionLevel(p, PotionEffectType.SPEED) > 0 && p.getNmsHorizontalDistance() >= Speed.ground) {
            preferred = 4.25;
        } else {
            double y = p.getNmsVerticalDistance();
            preferred = p.isJumping(y) || p.isFalling(y) ? 4.25 : defaultHorizontalReach;
        }
        return Math.max(ResearchEngine.getReach()[1], preferred);
    }

    private static double filter(double distance, double minimum, double extra) {
        return Math.max(Math.min(CombatUtils.maxHitDistance, distance + extra), minimum);
    }
}
