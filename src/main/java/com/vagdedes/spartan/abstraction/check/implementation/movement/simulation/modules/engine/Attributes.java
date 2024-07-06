package com.vagdedes.spartan.abstraction.check.implementation.movement.simulation.modules.engine;


import com.vagdedes.spartan.abstraction.player.SpartanPotionEffect;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import org.bukkit.potion.PotionEffectType;

public class Attributes
{
    public static Object getAttributeValue(Attribute attribute, SpartanProtocol protocol) {
        if (attribute.equals(Attribute.MOVEMENT_SPEED)) {
            float d = 0.1F * (protocol.player.getWalkSpeed() * 5);
            SpartanPotionEffect speedEffect = protocol.spartanPlayer.getPotionEffect(PotionEffectType.SPEED, 0);
            if (protocol.isSprinting()) {
                d *= 1.3F;
            }
            if (speedEffect != null) {
                d *= (float) (1 + (speedEffect.bukkitEffect.getAmplifier() + 1) * 0.2);
            }
            return d;
        }
        return null;
    }

}
