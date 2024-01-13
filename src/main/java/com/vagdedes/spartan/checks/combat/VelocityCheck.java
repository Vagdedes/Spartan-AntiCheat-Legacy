package com.vagdedes.spartan.checks.combat;

import com.vagdedes.spartan.abstraction.CheckExecutor;
import com.vagdedes.spartan.compatibility.manual.damage.NoHitDelay;
import com.vagdedes.spartan.compatibility.manual.essential.Essentials;
import com.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import com.vagdedes.spartan.objects.data.Handlers;
import com.vagdedes.spartan.objects.replicates.SpartanLocation;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;
import com.vagdedes.spartan.utils.gameplay.PatternUtils;

public class VelocityCheck extends CheckExecutor {

    private static final Enums.HackType check = Enums.HackType.Velocity;

    public VelocityCheck(SpartanPlayer player) {
        super(check, player);
    }

    private static final double[][]
            solid = new double[][]{
            {0, 1, 0},
            {0, 2, 0}
    }, web = new double[][]{
            {BlockUtils.hitbox_max, -1, BlockUtils.hitbox_max},
            {BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max}
    }, semiSolid = new double[][]{
            {BlockUtils.hitbox_max, -1, BlockUtils.hitbox_max},
            {BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max},
            {BlockUtils.hitbox_max, 1, BlockUtils.hitbox_max},
            {BlockUtils.hitbox_max, 2, BlockUtils.hitbox_max}
    };

    @Override
    public boolean run() {
        return false; //todo
    }

    @Override
    protected boolean canDo() {
        if (player.canRunCheck(hackType, true)
                && !player.isUsingItem()
                && !player.isDead()
                && !player.isInTheBorder()
                && !player.wasFlying()
                && !player.hasShield()
                && !player.wasInLiquids()
                && player.getBlocksOffGround(3, true, true) <= 2
                && !player.getHandlers().has(Handlers.HandlerType.Vehicle)
                && CombatUtils.getEnemiesNumber(player, CombatUtils.maxHitDistance, false, 6) <= 5 && !NoHitDelay.hasCooldown(player)
                && !Essentials.isAFK(player)
                && !Attributes.has(player, Attributes.GENERIC_KNOCKBACK_RESISTANCE)) {
            SpartanLocation loc = player.getLocation();

            if (!BlockUtils.canClimb(loc)) {
                return !PatternUtils.isBlockPattern(solid, loc, false, BlockUtils.solid, BlockUtils.semi_solid)
                        && !PatternUtils.isBlockPattern(web, loc, true, BlockUtils.web)
                        && !PatternUtils.isBlockPattern(semiSolid, loc, true, BlockUtils.semi_solid);
            }
        }
        return false;
    }

    @Override
    public boolean handle(Object object) {
        return false; //todo
    }
}
