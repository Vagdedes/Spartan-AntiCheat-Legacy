package com.vagdedes.vacan.functionality.tracking;

import com.vagdedes.vacan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.vacan.compatibility.Compatibility;
import com.vagdedes.vacan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.vacan.compatibility.manual.building.MythicMobs;
import com.vagdedes.vacan.compatibility.manual.vanilla.Attributes;
import com.vagdedes.vacan.utils.minecraft.entity.CombatUtils;
import org.bukkit.entity.Entity;

import java.util.List;

public class MovementProcessing {

    public static boolean canCheck(PlayerProtocol protocol,
                                   boolean vehicle,
                                   boolean elytra,
                                   boolean flight,
                                   boolean playerAttributes,
                                   boolean environmentalAttributes) {
        if ((elytra || !protocol.isGliding())
                && (flight || !protocol.wasFlying())
                && (vehicle || protocol.getVehicle() == null)

                && (playerAttributes
                || Attributes.getAmount(protocol, Attributes.GENERIC_MOVEMENT_SPEED) == 0.0
                && Attributes.getAmount(protocol, Attributes.GENERIC_JUMP_STRENGTH) == 0.0)

                && (environmentalAttributes
                || Attributes.getAmount(protocol, Attributes.GENERIC_STEP_HEIGHT) == 0.0
                && Attributes.getAmount(protocol, Attributes.GENERIC_GRAVITY) == 0.0
                && Attributes.getAmount(protocol, Attributes.GENERIC_STEP_HEIGHT) == 0.0)) {
            if (Compatibility.CompatibilityType.MYTHIC_MOBS.isFunctional()
                    || Compatibility.CompatibilityType.ITEMS_ADDER.isFunctional()) {
                List<Entity> entities = protocol.getNearbyEntities(
                        CombatUtils.maxHitDistance,
                        CombatUtils.maxHitDistance,
                        CombatUtils.maxHitDistance
                );

                if (!entities.isEmpty()) {
                    for (Entity entity : entities) {
                        if (MythicMobs.is(entity) || ItemsAdder.is(entity)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

}
