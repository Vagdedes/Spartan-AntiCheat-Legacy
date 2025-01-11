package com.vagdedes.spartan.abstraction.check.implementation.combat;

import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.definition.ImplementedProbabilityDetection;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.utils.math.RayUtils;
import com.vagdedes.spartan.utils.minecraft.entity.CombatUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.potion.PotionEffectType;

public class Criticals extends CheckRunner {

    private final CheckDetection detection;

    public Criticals(HackType hackType, SpartanProtocol protocol) {
        super(hackType, protocol);
        this.detection = new ImplementedProbabilityDetection(this, null, null, null, true);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        this.detection.call(() -> {
            double verticalMaxAccuracy = this.protocol.getLocation().getY() - this.protocol.getFromLocation().getY();

            if (RayUtils.onBlock(
                    this.protocol,
                    this.protocol.getLocationOrVehicle(),
                    BlockUtils.web
            )
            ) {
                return;
            }

            if ((verticalMaxAccuracy < 0.079 || Math.abs(verticalMaxAccuracy) == 0.0625)
                    && verticalMaxAccuracy > 0
                    && this.protocol.spartan.movement.getTicksOnAir() == 1
                    && !new SpartanLocation(this.protocol.getLocation()).getBlock().getType().isBlock()
                    && !RayUtils.onSolidBlock(this.protocol, this.protocol.getLocation().clone().add(0, 2, 0))) {
                this.detection.cancel("type: packet");
            }
        });
    }

    @Override
    protected boolean canRun() {
        return CombatUtils.canCheck(this.protocol)
                && !this.protocol.spartan.movement.isInLiquids()
                && !this.protocol.spartan.hasPotionEffect(PotionEffectType.BLINDNESS, 0);
    }
}
