package com.vagdedes.spartan.abstraction.check.implementation.combat;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.check.ImplementedDetectionExecutor;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.utils.math.RayUtils;
import com.vagdedes.spartan.utils.minecraft.entity.CombatUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.potion.PotionEffectType;

public class Criticals extends CheckExecutor {

    private final DetectionExecutor detection;

    public Criticals(HackType hackType, SpartanProtocol protocol) {
        super(hackType, protocol);
        this.detection = new ImplementedDetectionExecutor(this, null, true);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        Double verticalMaxAccuracy = this.protocol().spartanPlayer.movement.getEventVertical();

        if (verticalMaxAccuracy != null) {
            if (RayUtils.onBlock(this.protocol().spartanPlayer, this.protocol().spartanPlayer.movement.getLocation(), BlockUtils.web)) return;

            if ((verticalMaxAccuracy < 0.079 || Math.abs(verticalMaxAccuracy) == 0.0625)
                    && verticalMaxAccuracy > 0
                    && this.protocol().spartanPlayer.movement.getTicksOnAir() == 1
                    && !this.protocol().spartanPlayer.movement.getLocation().getBlock().getType().isBlock()
                    && !RayUtils.onSolidBlock(this.protocol().spartanPlayer, this.protocol().spartanPlayer.movement.getLocation().clone().add(0, 2, 0))) {
                this.detection.cancel("type: packet", 1);
            }
        }
    }

    @Override
    protected boolean canRun() {
        return CombatUtils.canCheck(this.protocol().spartanPlayer)
                && !this.protocol().spartanPlayer.movement.isInLiquids()
                && !this.protocol().spartanPlayer.hasPotionEffect(PotionEffectType.BLINDNESS, 0);
    }
}
