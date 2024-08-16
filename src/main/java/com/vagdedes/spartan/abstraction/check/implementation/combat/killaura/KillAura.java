package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.check.implementation.combat.killaura.movedirection.MoveDirection;
import com.vagdedes.spartan.abstraction.event.PlayerAttackEvent;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.CombatProcessing;
import com.vagdedes.spartan.utils.minecraft.entity.CombatUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.UUID;

public class KillAura extends CheckExecutor {

    private final Irregular irregular;
    private final EntityRaytrace entityRaytrace;
    private final BlockRaytrace blockRaytrace;
    private final HitTime hitTime;
    private final HitDirection hitDirection;
    private final HitDirectionML hitDirectionML;
    private final ResponseTime responseTime;
    private final MoveDirection moveDirection;
    private final MoveDirectionML moveDirectionML;
    private final FightAnalysis analysis;

    public KillAura(Enums.HackType hackType, SpartanPlayer player) {
        super(hackType, player, 10, false, false);
        this.addDetections(new DetectionExecutor[]{
                irregular = new Irregular(this),
                entityRaytrace = new EntityRaytrace(this),
                blockRaytrace = new BlockRaytrace(this),
                analysis = new FightAnalysis(this),
                hitTime = new HitTime(this),
                hitDirection = new HitDirection(this),
                responseTime = new ResponseTime(this),
                moveDirection = new MoveDirection(this),
                hitDirectionML = new HitDirectionML(this),
                moveDirectionML = new MoveDirectionML(this)
        });
    }

    private void runDetection(String option, Runnable runnable) {
        if (hackType.getCheck().getBooleanOption(option, true)) {
            runnable.run();
        }
    }

    boolean canRunEntity(LivingEntity entity) {
        if (entity.getVehicle() == null
                && CombatProcessing.canCheck(player, entity)
                && !CombatUtils.isEntityWide(CombatUtils.getWidthAndHeight(entity)[0])) {
            UUID entityUUID = entity.getUniqueId();
            boolean isPlayer = SpartanBukkit.isPlayer(entityUUID);
            return (isPlayer || !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_10) || entity.hasAI())
                    && !player.uuid.equals(entityUUID)
                    && hackType.getCheck().getBooleanOption("detection." + (isPlayer ? "players" : "entities"), true);
        }
        return false;
    }

    @Override
    protected void runInternal(boolean cancelled) {
        runDetection(
                "check_response_time",
                responseTime::runMove
        );
        runDetection(
                "check_move_direction_machine_learning",
                moveDirectionML::runMove
        );
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof PlayerMoveEvent) {
            runDetection(
                    "check_move_direction",
                    moveDirection::runMove
            );
        } else if (object instanceof PlayerAttackEvent) {
            PlayerAttackEvent event = (PlayerAttackEvent) object;
            LivingEntity target = event.getTarget();

            if (canRunEntity(target)) {
                runDetection(
                        "check_irregular",
                        irregular::runDamage
                );
                runDetection(
                        "check_block_raytrace",
                        () -> blockRaytrace.runDamage(target)
                );
                runDetection(
                        "check_entity_raytrace",
                        entityRaytrace::runDamage
                );
                runDetection(
                        "check_move_direction",
                        () -> moveDirection.runDamage(target)
                );
                runDetection(
                        "check_hit_time",
                        hitTime::runDamage
                );
                runDetection(
                        "check_hit_direction",
                        () -> hitDirection.runDamage(target)
                );
                runDetection(
                        "check_hit_direction_machine_learning",
                        hitDirectionML::runDamage
                );
                runDetection(
                        "check_response_time",
                        responseTime::runDamage
                );
                runDetection(
                        "check_response_time",
                        analysis::runDamage
                );
            }
        }
    }

    @Override
    protected boolean canRun() {
        return CombatProcessing.canCheck(player);
    }
}
