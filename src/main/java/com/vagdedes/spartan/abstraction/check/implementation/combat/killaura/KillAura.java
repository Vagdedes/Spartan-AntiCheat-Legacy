package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.profiling.PlayerFight;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.CombatProcessing;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.LivingEntity;

import java.util.UUID;

public class KillAura extends CheckExecutor {

    private final Irregular irregular;
    private final EntityRaytrace entityRaytrace;
    private final BlockRaytrace blockRaytrace;
    private final HitTime hitTime;
    private final HitDirection hitDirection;
    private final ResponseTime responseTime;
    private final MoveDirection moveDirection;
    private final MoveLength moveLength;
    private final FightAnalysis analysis;

    public KillAura(SpartanPlayer player) {
        super(Enums.HackType.KillAura, player, 9);
        this.addDetections(new DetectionExecutor[]{
                irregular = new Irregular(this),
                entityRaytrace = new EntityRaytrace(this),
                blockRaytrace = new BlockRaytrace(this),
                analysis = new FightAnalysis(this),
                hitTime = new HitTime(this),
                hitDirection = new HitDirection(this),
                responseTime = new ResponseTime(this),
                moveDirection = new MoveDirection(this),
                moveLength = new MoveLength(this)
        });
    }

    static final double
            minimumDistance = 0.1,
            normalWidthToPlayerView = 0.7,
            universalNmsDistanceRequirement = 0.2;
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

    void violateLive(String information) {
        cancel(information, null, (int) TPS.maximum);
        player.getViolations(hackType).prevent();
    }

    boolean canRunEntity(LivingEntity entity) {
        if (entity.getVehicle() == null
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
    public void runInternal(boolean cancelled) {
        if (canDo()) {
            SpartanLocation to = player.movement.getLocation();
            Buffer buffer = this.getBuffer();

            runDetection(
                    "check_aim",
                    () -> moveDirection.runMove(),
                    false
            );
            runDetection(
                    "check_sensitivity",
                    () -> moveLength.run(to, buffer),
                    false
            );
            runDetection(
                    "check_response",
                    () -> responseTime.runMove(),
                    false
            );
        }
    }

    @Override
    public void schedulerInternal() {
    }

    @Override
    public void handleInternal(boolean cancelled, Object object) {
        if (object instanceof PlayerFight) {
            if (canDo()) {
                analysis.run((PlayerFight) object);
            }
        } else {
            if (canDo()) {
                Object[] objects = (Object[]) object;
                LivingEntity entity = (LivingEntity) objects[0];

                if (canRunEntity(entity)) {
                    double[] utility = (double[]) objects[1];
                    double distance = utility[2];

                    if (distance >= minimumDistance) {
                        double width = utility[0],
                                height = utility[1];

                        if (width != 0.0
                                && height != 0.0) {
                            SpartanLocation location = player.movement.getLocation();

                            runDetection(
                                    "check_irregular",
                                    irregular::run,
                                    true
                            );
                            runDetection(
                                    "check_block_raytrace",
                                    () -> blockRaytrace.run(),
                                    true
                            );
                            runDetection(
                                    "check_entity_raytrace",
                                    () -> entityRaytrace.run(),
                                    true
                            );
                            runDetection(
                                    "check_time",
                                    () -> hitTime.run(),
                                    true
                            );
                            runDetection(
                                    "check_direction",
                                    () -> hitDirection.run(),
                                    true
                            );
                            runDetection(
                                    "check_response",
                                    () -> responseTime.run(),
                                    true
                            );
                        }
                    }
                }
            }
        }
    }

    @Override
    protected boolean canDo() {
        return CombatProcessing.canCheck(player);
    }
}
