package com.vagdedes.spartan.abstraction.check.implementation.combat;

import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.definition.ImplementedProbabilityDetection;
import com.vagdedes.spartan.abstraction.event.PlayerAttackEvent;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.math.RayUtils;
import com.vagdedes.spartan.utils.minecraft.entity.CombatUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;

public class HitReach extends CheckRunner {

    private final CheckDetection
            hitboxRaytrace,
            simple;
    private final LinkedList<Double> rayTraceAnalysis = new LinkedList<>();
    private int vl = 0;
    private int localVl = 0;

    public HitReach(HackType hackType, SpartanProtocol protocol) {
        super(hackType, protocol);
        hitboxRaytrace = new ImplementedProbabilityDetection(this, "hitbox_raytrace", true);
        simple = new ImplementedProbabilityDetection(this, "simple", true);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        PlayerAttackEvent event = (PlayerAttackEvent) object;
        if (!(event.target instanceof Player)) return;
        if (this.protocol().packetsEnabled()) {
            if (CombatUtils.canCheck(this.protocol(), SpartanBukkit.getProtocol((Player) event.target).bukkit)
                    && !this.protocol().bukkit.getGameMode().equals(GameMode.CREATIVE)) {
                SpartanProtocol target = SpartanBukkit.getProtocol((Player) event.target);
                List<Location> h = target.positionHistory;
                boolean is1_8 = this.protocol().isUsingVersion(MultiVersion.MCVersion.V1_8);
                float hitbox = (is1_8) ? 0.4F : 0.3F;
                boolean isHit = RayUtils.inHitbox(this.protocol(), target.getLocation(), event.target, hitbox)
                                || RayUtils.inHitboxFrom(this.protocol(), target.getLocation(), event.target, hitbox);;
                int i = 0;
                for(Location l : h) {
                    if (RayUtils.inHitbox(this.protocol(), l, event.target, hitbox)
                                    || RayUtils.inHitboxFrom(this.protocol(), l, event.target, hitbox)) {
                        break;
                    } else {
                        i++;
                    }
                }
               // this.protocol().bukkit.sendMessage("i: " + i);
                if (!isHit && i == 20) {
                  // this.protocol().bukkit.sendMessage("flag");
                }
            }
        } else {
            LivingEntity target = event.target;
            if (CombatUtils.canCheck(this.protocol(), target)
                    && !this.protocol().bukkit.getGameMode().equals(GameMode.CREATIVE)) {
                double bruteforceRayTrace = RayUtils.bruteforceRayTrace(this.protocol(), target),
                        diff = bruteforceRayTrace - 0.4;
                rayTraceAnalysis.addLast(bruteforceRayTrace);
                if (rayTraceAnalysis.size() >= 10) {
                    analyze(this.protocol().spartan.movement.getLocation(), bruteforceRayTrace);
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

        if (result > (this.protocol().packetsEnabled() ? 0.43 : 0.5)) punish = "simple";
        int percent = (int) ((result / 0.4) * 100);
        if (punish != null) {
            vl += 100;
            this.simple.cancel( // ATTENTION: Add more detections to the check if you write more definitions for the 'punish' String field
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
        return CombatUtils.canCheck(this.protocol());
    }
}
