package com.vagdedes.spartan.abstraction.check.implementation.combat;

import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.def.ImplementedProbabilityDetection;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.utils.math.RayUtils;
import com.vagdedes.spartan.utils.minecraft.entity.CombatUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.potion.PotionEffectType;

public class Criticals extends CheckRunner {

    private final CheckDetection detection;

    public Criticals(HackType hackType, SpartanProtocol protocol) {
        super(hackType, protocol);
        this.detection = new ImplementedProbabilityDetection(this, null, true);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        Double verticalMaxAccuracy = this.protocol().spartan.movement.getEventVertical();

        if (verticalMaxAccuracy != null) {
            if (RayUtils.onBlock(
                    this.protocol(),
                    this.protocol().spartan.movement.getLocation().bukkit(),
                    BlockUtils.web
            )
            ) {
                return;
            }

            if ((verticalMaxAccuracy < 0.079 || Math.abs(verticalMaxAccuracy) == 0.0625)
                    && verticalMaxAccuracy > 0
                    && this.protocol().spartan.movement.getTicksOnAir() == 1
                    && !this.protocol().spartan.movement.getLocation().getBlock().getType().isBlock()
                    && !RayUtils.onSolidBlock(this.protocol(), this.protocol().spartan.movement.getLocation().clone().add(0, 2, 0).bukkit())) {
                this.detection.cancel("type: packet", 1);
            }
        }
    }

    @Override
    protected boolean canRun() {
        return CombatUtils.canCheck(this.protocol())
                && !this.protocol().spartan.movement.isInLiquids()
                && !this.protocol().spartan.hasPotionEffect(PotionEffectType.BLINDNESS, 0);
    }
}
