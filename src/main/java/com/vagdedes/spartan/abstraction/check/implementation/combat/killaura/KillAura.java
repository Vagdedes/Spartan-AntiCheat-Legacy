package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.check.implementation.combat.killaura.movedirection.MoveDirection;
import com.vagdedes.spartan.abstraction.event.EntityAttackPlayerEvent;
import com.vagdedes.spartan.abstraction.event.PlayerAttackEvent;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.tracking.CombatProcessing;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

public class KillAura extends CheckExecutor {

    private final Irregular irregular;
    private final EntityRaytrace entityRaytrace;
    private final BlockRaytrace blockRaytrace;
    private final HitTime hitTime;
    private final HitDirection hitDirection;
    private final ResponseTime responseTime;
    private final MoveDirection moveDirection;
    private final FightAnalysis analysis;

    public KillAura(Enums.HackType hackType, SpartanPlayer player) {
        super(hackType, player, 8);
        this.addDetections(new DetectionExecutor[]{
                irregular = new Irregular(this),
                entityRaytrace = new EntityRaytrace(this),
                blockRaytrace = new BlockRaytrace(this),
                analysis = new FightAnalysis(this),
                hitTime = new HitTime(this),
                hitDirection = new HitDirection(this),
                responseTime = new ResponseTime(this),
                moveDirection = new MoveDirection(this)
        });
    }

    private void runDetection(String option, Runnable runnable) {
        if (hackType.getCheck().getBooleanOption(option, true)) {
            runnable.run();
        }
    }

    boolean canRunEntity(LivingEntity entity) {
        if (entity.getVehicle() == null
                && CombatProcessing.canCheck(player, entity)) {
            boolean isPlayer = entity instanceof Player;
            return (isPlayer || !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_10) || entity.hasAI())
                    && !player.getInstance().equals(entity)
                    && hackType.getCheck().getBooleanOption("detection." + (isPlayer ? "players" : "entities"), true);
        }
        return false;
    }

    @Override
    protected void runInternal(boolean cancelled) {
        runDetection(
                "check_response_time",
                responseTime::run
        );
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof PlayerMoveEvent) {
            runDetection(
                    "check_move_direction",
                    moveDirection::run
            );
        } else if (object instanceof PlayerAttackEvent) {
            PlayerAttackEvent event = (PlayerAttackEvent) object;
            LivingEntity target = event.getTarget();

            if (canRunEntity(target)) {

                runDetection(
                        "check_irregular",
                        irregular::run
                );
                runDetection(
                        "check_block_raytrace",
                        () -> blockRaytrace.run(target)
                );
                runDetection(
                        "check_entity_raytrace",
                        entityRaytrace::run
                );
                runDetection(
                        "check_move_direction",
                        () -> moveDirection.run(target)
                );
                runDetection(
                        "check_hit_time",
                        hitTime::run
                );
                runDetection(
                        "check_hit_direction",
                        hitDirection::run
                );
                runDetection(
                        "check_fight_analysis",
                        () -> analysis.runDealDamage(event)
                );
            }
        } else if (object instanceof EntityAttackPlayerEvent) {
            EntityAttackPlayerEvent event = (EntityAttackPlayerEvent) object;
            runDetection(
                    "check_fight_analysis",
                    () -> analysis.runReceiveDamage(event)
            );
        }
    }

    @Override
    protected boolean canRun() {
        return CombatProcessing.canCheck(player);
    }
}
