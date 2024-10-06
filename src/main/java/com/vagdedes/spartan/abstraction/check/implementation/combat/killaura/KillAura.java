package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.implementation.combat.killaura.movedirection.MoveDirection;
import com.vagdedes.spartan.abstraction.event.EntityAttackPlayerEvent;
import com.vagdedes.spartan.abstraction.event.PlayerAttackEvent;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.minecraft.entity.CombatUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

public class KillAura extends CheckExecutor {

    private final Irregular irregular;
    private final EntityRaytrace entityRaytrace;
    private final BlockRaytrace blockRaytrace;
    private final HitTime hitTime;
    private final HitDirection hitDirection;
    private final ResponseTime responseTime;
    private final MoveDirection moveDirection;
    private final FightAnalysis analysis;

    public KillAura(Enums.HackType hackType, SpartanPlayer player) {
        super(hackType, player);
        irregular = new Irregular(this);
        entityRaytrace = new EntityRaytrace(this);
        blockRaytrace = new BlockRaytrace(this);
        analysis = new FightAnalysis(this);
        hitTime = new HitTime(this);
        hitDirection = new HitDirection(this);
        responseTime = new ResponseTime(this);
        moveDirection = new MoveDirection(this);
    }

    boolean canRunEntity(LivingEntity entity) {
        if (entity.getVehicle() == null
                && CombatUtils.canCheck(player, entity)) {
            boolean isPlayer = entity instanceof Player;
            return (isPlayer || !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_10) || entity.hasAI())
                    && !player.getInstance().equals(entity)
                    && hackType.getCheck().getBooleanOption("detection." + (isPlayer ? "players" : "entities"), true);
        }
        return false;
    }

    @Override
    protected void runInternal(boolean cancelled) {
        responseTime.run();
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof PlayerMoveEvent) {
            moveDirection.run();
        } else if (object instanceof PlayerAttackEvent) {
            PlayerAttackEvent event = (PlayerAttackEvent) object;
            LivingEntity target = event.target;

            if (canRunEntity(target)) {

                irregular.run();
                blockRaytrace.run(target);
                entityRaytrace.run();
                moveDirection.run(target);
                hitTime.run();
                hitDirection.run();
                analysis.runDealDamage(event);
            }
        } else if (object instanceof EntityAttackPlayerEvent) {
            EntityAttackPlayerEvent event = (EntityAttackPlayerEvent) object;
            analysis.runReceiveDamage(event);
        }
    }

    @Override
    protected boolean canRun() {
        return CombatUtils.canCheck(player);
    }
}
