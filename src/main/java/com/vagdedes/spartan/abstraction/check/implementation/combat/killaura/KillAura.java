package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.event.PlayerAttackEvent;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.profiling.PlayerFight;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.CombatProcessing;
import com.vagdedes.spartan.utils.minecraft.server.CombatUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.LivingEntity;

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
    private final MoveLength moveLength;
    private final FightAnalysis analysis;

    public KillAura(Enums.HackType hackType, SpartanPlayer player) {
        super(hackType, player, 11);
        this.addDetections(new DetectionExecutor[]{
                irregular = new Irregular(this),
                entityRaytrace = new EntityRaytrace(this),
                blockRaytrace = new BlockRaytrace(this),
                analysis = new FightAnalysis(this),
                hitTime = new HitTime(this),
                hitDirection = new HitDirection(this),
                responseTime = new ResponseTime(this),
                moveDirection = new MoveDirection(this),
                moveLength = new MoveLength(this),
                hitDirectionML = new HitDirectionML(this),
                moveDirectionML = new MoveDirectionML(this)
        });
    }

    static final long pastDamageMilliseconds = 1_000L;

    private void runDetection(String option, Runnable runnable, boolean sync) {
        if (hackType.getCheck().getBooleanOption(option, true)) {
            if (sync) {
                runnable.run();
            } else {
                runAsync(runnable);
            }
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
                "check_move_length",
                moveLength::runMove,
                false
        );
        runDetection(
                "check_response_time",
                responseTime::runMove,
                false
        );
        runDetection(
                "check_move_direction",
                moveDirection::runMove,
                false
        );
        runDetection(
                "check_move_direction_machine_learning",
                moveDirectionML::runMove,
                false
        );
    }

    @Override
    public void schedulerInternal() {
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof PlayerFight) {
            analysis.run((PlayerFight) object);
        } else {
            PlayerAttackEvent event = (PlayerAttackEvent) object;

            if (canRunEntity(event.getTarget())) {
                runDetection(
                        "check_irregular",
                        irregular::run,
                        true
                );
                runDetection(
                        "check_block_raytrace",
                        blockRaytrace::run,
                        true
                );
                runDetection(
                        "check_entity_raytrace",
                        entityRaytrace::run,
                        true
                );
                runDetection(
                        "check_hit_time",
                        hitTime::run,
                        true
                );
                runDetection(
                        "check_hit_direction",
                        hitDirection::run,
                        true
                );
                runDetection(
                        "check_hit_direction_machine_learning",
                        hitDirectionML::run,
                        true
                );
                runDetection(
                        "check_response_time",
                        responseTime::run,
                        true
                );
            }
        }
    }

    @Override
    protected boolean canRun() {
        return CombatProcessing.canCheck(player);
    }
}
