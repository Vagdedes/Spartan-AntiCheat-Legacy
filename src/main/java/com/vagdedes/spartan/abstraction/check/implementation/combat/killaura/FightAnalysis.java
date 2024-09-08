package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.event.EntityAttackPlayerEvent;
import com.vagdedes.spartan.abstraction.event.PlayerAttackEvent;
import com.vagdedes.spartan.utils.java.OverflowMap;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FightAnalysis extends DetectionExecutor {

    private static final Map<UUID, Map<String, int[]>> data = new OverflowMap<>(
            new HashMap<>(2),
            1_024
    );

    FightAnalysis(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void runDealDamage(PlayerAttackEvent e) {
        LivingEntity target = e.getTarget();
        String identifier;

        if (target instanceof Player) {
            identifier = target.getName();
        } else {
            identifier = target.getType().toString();
        }
        Map<String, int[]> subData = data.computeIfAbsent(
                player.protocol.getUUID(),
                k -> new HashMap<>(2)
        );
        int[] hitsDamages = subData.getOrDefault(identifier, new int[2]);
        hitsDamages[0]++;
        subData.put(identifier, hitsDamages);
    }

    void runReceiveDamage(EntityAttackPlayerEvent e) {
        LivingEntity damager = e.getDamager();
        String identifier;

        if (damager instanceof Player) {
            identifier = damager.getName();
        } else {
            identifier = damager.getType().toString();
        }
        Map<String, int[]> subData = data.computeIfAbsent(
                player.protocol.getUUID(),
                k -> new HashMap<>(2)
        );
        int[] hitsDamages = subData.getOrDefault(identifier, new int[2]);
        hitsDamages[1]++;
        subData.put(identifier, hitsDamages);
    }

}
