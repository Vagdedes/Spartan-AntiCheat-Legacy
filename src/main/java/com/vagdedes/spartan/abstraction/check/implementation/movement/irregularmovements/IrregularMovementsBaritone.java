package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.ProbabilityDetection;
import com.vagdedes.spartan.utils.math.RayUtils;
import com.vagdedes.spartan.utils.minecraft.vector.SpartanVector2F;
import org.bukkit.Location;

public class IrregularMovementsBaritone extends ProbabilityDetection {

    IrregularMovementsBaritone(CheckRunner executor) {
        super(executor, "baritone", true);
    }

    private SpartanVector2F oldV2 = new SpartanVector2F(0, 0);
    private SpartanVector2F oldDeltaMap = new SpartanVector2F(0, 0);
    private float yawMoved = 0;
    private int vl = 0;

    void run() {
        if (!this.protocol().spartan.isBedrockPlayer()) {
            Location l = this.protocol().getLocation();
            SpartanVector2F v2 = new SpartanVector2F(l.getYaw(), l.getPitch());
            float deltaYaw = (float) RayUtils.scaleVal(Math.abs(v2.x - oldV2.x), 3);
            float deltaPitch = (float) RayUtils.scaleVal(Math.abs(v2.y - oldV2.y), 3);
            SpartanVector2F deltaMap = new SpartanVector2F(deltaYaw, deltaPitch);

            // for future
            float jiffDeltaYaw = (float) RayUtils.scaleVal(Math.abs(deltaYaw - oldDeltaMap.x), 3);
            float jiffDeltaPitch = (float) RayUtils.scaleVal(Math.abs(deltaPitch - oldDeltaMap.y), 3);
            {
                if (this.protocol().bukkit.getVehicle() != null) return;
                this.yawMoved += (Math.abs(l.getPitch()) > 89) ? 0 : (deltaPitch == 0.0F) ? deltaYaw : -yawMoved;
                this.yawMoved = (float) RayUtils.scaleVal(yawMoved, 2);
                if (this.yawMoved > 45 && Math.abs(yawMoved % 360.0 - 1.0) < 1e-9) {
                    this.vl += 3;
                    if (this.vl > 14)
                        punish(3, "type: baritone[rotation], euler-y-total: " + this.yawMoved);
                }
                //player.getInstance().sendMessage(jiffDeltaYaw + " " + jiffDeltaPitch);
            }
            if (this.vl > 0) this.vl--;
            this.oldV2 = v2;
            this.oldDeltaMap = deltaMap;
        }
    }

    void teleport() {
        this.yawMoved = 0;
    }

    private void punish(int v, String s) {
        this.vl += v;
        if (this.vl > 10) {
            cancel(s);
            this.vl -= 4;
        }
    }

}
