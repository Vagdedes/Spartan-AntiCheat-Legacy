package com.vagdedes.spartan.checks.combat.killAura;

// Study of usually legitimate but abnormally long and consistent distance

import com.vagdedes.spartan.abstraction.CheckExecutor;
import com.vagdedes.spartan.abstraction.DetectionExecutor;
import com.vagdedes.spartan.compatibility.manual.damage.NoHitDelay;
import com.vagdedes.spartan.objects.data.Buffer;
import com.vagdedes.spartan.objects.profiling.PlayerFight;
import com.vagdedes.spartan.objects.profiling.PlayerOpponent;
import com.vagdedes.spartan.objects.replicates.SpartanLocation;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;
import com.vagdedes.spartan.utils.gameplay.MoveUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class Distance extends DetectionExecutor {

    Distance(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    private static final String key = Distance.class.getName();
    private static final int groundBlockRequirement = 2;

    void run(SpartanLocation location, LivingEntity entity, double distance, boolean isFightParticipant) {
        if (!NoHitDelay.hasCooldown(player)
                && (!isFightParticipant || player.getProfile().playerCombat.isActivelyFighting(null, false, true, false)) // We want the player in active combat or just starting combat
                && CombatUtils.areFacingEachOther(player, entity)
                && !CombatUtils.isNewPvPMechanic(player, entity)
                && player.getBlocksOffGround(groundBlockRequirement + 1, true, true) <= groundBlockRequirement // We need the player close to the ground
                && entity instanceof Player) {
            SpartanPlayer targetPlayer = SpartanBukkit.getPlayer((Player) entity);

            if (targetPlayer != null
                    && !NoHitDelay.hasCooldown(targetPlayer)
                    && !CombatUtils.hasBlockBehind(player, entity)
                    && targetPlayer.getBlocksOffGround(groundBlockRequirement + 1, true, true) <= groundBlockRequirement) { // We need the target close to the ground
                PlayerFight fight = player.getProfile().playerCombat.getFight(targetPlayer);

                if (fight.getHitRatio(player) > 1.0) { // Make sure the player has a hit advantage
                    PlayerOpponent[] opponents = fight.getOpponent(player);

                    if (opponents[0].hasHitCombo()) { // Make sure the player has a hit combo
                        long timePassed = opponents[0].getLastHit(false);

                        if (timePassed <= 755L) { // 15 Ticks (Make sure the previous hit is recent or non-existent)
                            double limit = MoveUtils.calculateLimit(player, location,
                                    Math.max(CombatUtils.getWidthAndHeight(entity)[0], 2.5),
                                    4.0, PotionEffectType.SPEED);

                            if (distance >= limit) { // Make sure the player's average distance is appropriate
                                Buffer buffer = player.getBuffer(hackType);

                                if (buffer.start(key, CombatUtils.getFightTicks(player) * 6) == 3) {
                                    buffer.remove(key);
                                    cancel("type: distance"
                                            + ", time-passed: " + timePassed
                                            + ", diagonal-distance: " + distance
                                            + ", distance: " + distance
                                            + ", limit: " + limit);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
