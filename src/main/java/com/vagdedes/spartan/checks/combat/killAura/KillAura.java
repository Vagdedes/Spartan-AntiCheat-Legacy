package com.vagdedes.spartan.checks.combat.killAura;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.data.Handlers;
import com.vagdedes.spartan.abstraction.profiling.PlayerFight;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.abilities.mcMMO;
import com.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.LivingEntity;

import java.util.UUID;

public class KillAura extends CheckExecutor {

    private final Irregular irregular;
    private final Raytrace raytrace;
    private final Distance distance;
    private final Time time;
    private final Rotations rotations;
    private final Direction direction;
    private final Movement movement;
    private final Response response;
    private final Pattern pattern;
    private final Aim aim;
    private final Sensitivity sensitivity;
    private final Strafe strafe;
    private final Analysis analysis;

    public KillAura(SpartanPlayer player) {
        super(Enums.HackType.KillAura, player, 13);
        this.addDetections(new DetectionExecutor[]{
                new Irregular(this),
                new Raytrace(this),
                new Distance(this),
                new Time(this),
                new Rotations(this),
                new Direction(this),
                new Movement(this),
                new Response(this),
                new Pattern(this),
                new Aim(this),
                new Sensitivity(this),
                new Strafe(this),
                new Analysis(this)
        });
        irregular = (Irregular) detections[0];
        raytrace = (Raytrace) detections[1];
        distance = (Distance) detections[2];
        time = (Time) detections[3];
        rotations = (Rotations) detections[4];
        direction = (Direction) detections[5];
        movement = (Movement) detections[6];
        response = (Response) detections[7];
        pattern = (Pattern) detections[8];
        aim = (Aim) detections[9];
        sensitivity = (Sensitivity) detections[10];
        strafe = (Strafe) detections[11];
        analysis = (Analysis) detections[12];
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
        cancel(information, null, CombatUtils.getFightTicks(player));
        player.getViolations(hackType).process();
    }

    void violateAll(String information) {
        cancel(information,
                null, 0, false,
                0.0, 2);
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
    public boolean runInternal(boolean cancelled) {
        if (canDo()) {
            SpartanLocation to = player.getEventToLocation(),
                    from = player.getEventFromLocation();
            Buffer buffer = player.getBuffer(hackType);

            runDetection(
                    "check_aim",
                    () -> aim.runMove(to, from, buffer, pattern),
                    false
            );
            runDetection(
                    "check_sensitivity",
                    () -> sensitivity.run(to, buffer),
                    false
            );
            runDetection(
                    "check_response",
                    () -> response.runMove(buffer),
                    false
            );
            runDetection(
                    "check_strafe",
                    () -> strafe.run(to, buffer),
                    false
            );
            runDetection(
                    "check_pattern",
                    () -> pattern.runMove(from),
                    false
            );
        }
        return false;
    }

    @Override
    public boolean handleInternal(boolean cancelled, Object object) {
        if (object == null) {
            aim.remove();
            sensitivity.remove();
            strafe.remove();
            pattern.remove();
        } else if (object instanceof PlayerFight) {
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
                            SpartanLocation location = player.getLocation();
                            boolean isFightParticipant = player.getProfile().playerCombat.isFighting();

                            runDetection(
                                    "check_irregular",
                                    irregular::run,
                                    true
                            );
                            runDetection(
                                    "check_raytrace",
                                    () -> raytrace.run(location, entity, distance),
                                    true
                            );
                            runDetection(
                                    "check_distance",
                                    () -> this.distance.run(location, entity, distance, isFightParticipant),
                                    true
                            );
                            runDetection(
                                    "check_time",
                                    () -> time.run(entity, width, distance),
                                    true
                            );
                            runDetection(
                                    "check_rotations",
                                    () -> rotations.run(entity, width),
                                    true
                            );
                            runDetection(
                                    "check_direction",
                                    () -> direction.run(entity, width, height, distance),
                                    true
                            );
                            runDetection(
                                    "check_movement",
                                    () -> movement.run(entity, distance, width),
                                    true
                            );
                            runDetection(
                                    "check_response",
                                    () -> response.run(entity, width, distance),
                                    true
                            );
                            runDetection(
                                    "check_pattern",
                                    () -> pattern.run(entity, location),
                                    true
                            );
                            runDetection(
                                    "check_aim",
                                    () -> aim.run(entity, location, width, height),
                                    true
                            );
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
                && !player.movement.isCrawling()
                && !player.movement.wasFlying()
                && !player.getHandlers().has(Handlers.HandlerType.ElytraUse)
                && !mcMMO.hasGeneralAbility(player)
                && !Attributes.has(player, Attributes.GENERIC_ARMOR);
    }
}
