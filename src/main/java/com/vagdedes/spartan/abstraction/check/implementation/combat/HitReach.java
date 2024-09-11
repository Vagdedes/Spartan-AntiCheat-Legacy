package com.vagdedes.spartan.abstraction.check.implementation.combat;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.implementation.movement.simulation.modules.engine.Attribute;
import com.vagdedes.spartan.abstraction.event.PlayerAttackEvent;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.CombatProcessing;
import com.vagdedes.spartan.listeners.protocol.standalone.Packet_LagCompensation;
import com.vagdedes.spartan.utils.math.RayUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.LinkedList;

public class HitReach extends CheckExecutor {

    private final LinkedList<Double> rayTraceAnalysis = new LinkedList<>();
    private int vl = 0;
    private int localVl = 0;

    public HitReach(HackType hackType, SpartanPlayer player) {
        super(hackType, player, 0, false);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        PlayerAttackEvent event = (PlayerAttackEvent) object;
        LivingEntity target = event.getTarget();

        if (player.protocol.packetsEnabled()) {
            if (CombatProcessing.canCheck(player, target)
                    && !player.getInstance().getGameMode().equals(GameMode.CREATIVE)) {
                int targetId = target.getEntityId();
                int playerId = event.getPlayer().getEntityId();
                SpartanProtocol targetProtocol = SpartanBukkit.getProtocol(event.getTarget().getUniqueId());
                if (targetProtocol == null) return;
                double bruteforceRayTrace = 14.88;
                double value;

                boolean is1_8 = true;
                double hitbox = (is1_8) ? 0.4 : 0.3;

                for (Location targetLocation : targetProtocol.getLocations()) {
                    if (targetLocation != null) {
                        value = RayUtils.bruteforceRayTraceWithCustomLocation(
                                player,
                                targetLocation,
                                target
                        );

                        if (value < bruteforceRayTrace) {
                            bruteforceRayTrace = value;
                        }
                    }
                }
                if (bruteforceRayTrace > hitbox + 0.01 && bruteforceRayTrace != 14.88) {
                    localVl += (int) (200 + ((bruteforceRayTrace - hitbox) * 100));
                    if (localVl > 300) {
                        cancel(
                                "type: hitbox-raytrace, reach: " + (bruteforceRayTrace - hitbox) + 3,
                                vl / 100D,
                                player.movement.getDetectionLocation()
                        );
                    }
                    localVl -= 30;
                } else if (localVl > 0) {
                    localVl -= 25;
                }


                /*

                double d = RayUtils.hitBoxRay(
                                player.getInstance(), player.movement.getEventToLocation().getBukkitLocation(),
                                LagCompensation.getLocationWithTickRollback(targetId, 0), 0.8, player.movement.isSneaking());

                 */
            }
        } else {
            if (CombatProcessing.canCheck(player, target)
                    && !player.getInstance().getGameMode().equals(GameMode.CREATIVE)) {
                double bruteforceRayTrace = RayUtils.bruteforceRayTrace(player, target),
                        diff = bruteforceRayTrace - 0.4;
                rayTraceAnalysis.addLast(bruteforceRayTrace);
                if (rayTraceAnalysis.size() >= 10) {
                    analyze(player.movement.getLocation(), bruteforceRayTrace);
                }
            }
        }
    }

    public void analyze(SpartanLocation to, double bruteForce) {
        String punish = null;
        double globalValue = 0;

        for (Double value : rayTraceAnalysis) {
            globalValue += value;
        }
        double result = globalValue / rayTraceAnalysis.size();

        if (result > (player.protocol.packetsEnabled() ? 0.43 : 0.5)) punish = "simple";
        int percent = (int) ((result / 0.4) * 100);
        if (punish != null) {
            vl += 100;
            cancel(
                    "type: " + punish + ", hitbox-size-analysis: "
                            + result + ", hitbox-size-last: " + bruteForce + ", percent-of-deviance: "
                            + percent + "%, reach: " + RayUtils.scaleVal((result / 0.4) * 3, 2) + " blocks",
                    vl / 100D,
                    to
            );
        } else {
            if (vl > 0) vl -= 50;
        }
        rayTraceAnalysis.clear();

    }

    @Override
    protected boolean canRun() {
        return CombatProcessing.canCheck(player);
    }
}
