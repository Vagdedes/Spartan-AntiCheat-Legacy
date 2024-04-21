package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.abstraction.data.Decimals;
import com.vagdedes.spartan.abstraction.data.Handlers;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class MovementProcessing {

    private static final boolean
            v1_8 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8),
            v1_12 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_12);
    private static final int roundedFall = AlgebraUtils.integerCeil(PlayerUtils.terminalVelocity);

    /*if (!Liquid.runMove(p)) {
            if (!GroundCollision.run(p)) {
                ExtremeCollision.run(p);
                BouncingBlocks.runMove(p);
            }
        } else {
            ExtremeCollision.run(p);
            BouncingBlocks.runMove(p);
        }*/

    public static void run(Player n, SpartanPlayer p,
                           double dis, double hor, double ver, double box,
                           float fall) {
        if (n.isSprinting()) {
            p.movement.setSprinting(true);
            p.movement.setSneaking(false);
        } else {
            p.movement.setSprinting(false);
            p.movement.setSneaking(n.isSneaking());
        }
        p.setFallDistance(n.getFallDistance(), false);
        p.setWalkSpeed(n.getWalkSpeed());
        p.setFlySpeed(n.getFlySpeed());
        p.setEyeHeight(n.getEyeHeight());
        p.setUsingItem(n.isBlocking());

        // Separator

        GameMode current = p.getGameMode();

        if (current == GameMode.CREATIVE
                || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8) && current == GameMode.SPECTATOR) {
            p.getHandlers().add(Handlers.HandlerType.GameMode, 60);
        }

        // Separator

        p.setVehicle(n.getVehicle());
        p.movement.setNmsDistance(dis, hor, ver, box, fall);

        // Jump/Fall Identifier
        if (Math.abs(PlayerUtils.jumping[1] - ver) < PlayerUtils.getJumpingPrecision(p)) { // Last Jump
            p.movement.setLastJump();
        } else if (p.movement.isFalling(ver)) { // Last Fall
            p.movement.setLastFall();
        }
        extremeCollision(p);

        // Extra Packets
        String key = "player-data=extra-packets";
        double difference = p.movement.getCustomDistance() - dis;
        p.getDecimals().add(key, difference, AlgebraUtils.integerRound(TPS.maximum));

        if (p.getBuffer().increase(key, 1) >= TPS.maximum) {
            p.getBuffer().remove(key);

            if (p.getDecimals().get(key, 0.0, Decimals.CALCULATE_AVERAGE) >= 0.01) {
                p.movement.setExtraPackets(p.movement.getExtraPackets() + 1);
            } else {
                p.movement.setExtraPackets(0);
            }
        }
    }

    private static boolean groundCollision(SpartanPlayer player,
                                           double vertical, double box) {
        if (player.isOnGroundCustom()
                && player.isOnGround()
                && player.movement.getTicksOnAir() == 0
                && player.getVehicle() == null
                && vertical == 0.0
                && GroundUtils.collisionHeightExists(box)) {
            Handlers handlers = player.getHandlers();
            handlers.removeMany(Handlers.HandlerFamily.Motion);
            handlers.removeMany(Handlers.HandlerFamily.Velocity);
            player.movement.removeLastLiquidTime();
            return true;
        } else {
            return false;
        }
    }

    static void extremeCollision(SpartanPlayer player) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            Handlers handlers = player.getHandlers();

            if (!handlers.isDisabled(Handlers.HandlerType.ExtremeCollision)) {
                handlers.disable(Handlers.HandlerType.ExtremeCollision, 2); // Disable for the next tick

                if (PlayerUtils.getNearbyCollisions(player) >= 0.9) {
                    handlers.add(Handlers.HandlerType.ExtremeCollision, 40);
                }
            }
        }
    }

    static void calculateBouncing(SpartanPlayer p, double vertical) {
        if (v1_8 && vertical != 0.0) {
            SpartanLocation location = p.movement.getLocation();

            if (BlockUtils.isSlime(p, location, roundedFall)) {
                p.getHandlers().add(Handlers.HandlerType.BouncingBlocks, "slime", 30);
            } else if (BlockUtils.isBed(p, location, roundedFall)) {
                p.getHandlers().add(Handlers.HandlerType.BouncingBlocks, "bed", 30);
            }
        }
    }

    public static boolean recentlyBounced(SpartanPlayer p) {
        return v1_8 && p.getHandlers().has(Handlers.HandlerType.BouncingBlocks, "slime")
                || v1_12 && p.getHandlers().has(Handlers.HandlerType.BouncingBlocks, "bed");
    }

}
