package com.vagdedes.spartan.objects.statistics;

import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

import java.util.*;

public class PatternStorage {

    private final String key;
    private final int[] generalizations;
    private final boolean generalization;
    private final Map<Integer, Collection<PatternValue>> available, hidden;
    private final Map<UUID, Map<Integer, Collection<PatternValue>>> correlations;
    private static final Object synchronizer = new Object();

    public PatternStorage(String key, int[] generalizations) {
        this.key = key;
        this.generalizations = generalizations;
        this.generalization = generalizations.length > 0;
        this.correlations = new LinkedHashMap<>();

        this.available = new LinkedHashMap<>();
        this.available.put(0, new ArrayList<>());

        this.hidden = new LinkedHashMap<>();
        this.hidden.put(0, new ArrayList<>());

        for (int generalization : generalizations) {
            this.available.put(generalization, new ArrayList<>());
            this.hidden.put(generalization, new ArrayList<>());
        }
    }

    public void store() {

    }

    // Separator

    private void rawCache(Map<Integer, Collection<PatternValue>> map,
                         SpartanPlayer player, PatternValue value, int generalization) {
        map.get(generalization).add(value);
        this.correlations.computeIfAbsent(
                player.getUniqueId(),
                k -> new LinkedHashMap<>()
        ).computeIfAbsent(
                generalization,
                k -> new ArrayList<>()
        ).add(value);
    }

    public void cache(SpartanPlayer player, Number number) {
        PatternValue value = new PatternValue(number);

        synchronized (synchronizer) {
            Map<Integer, Collection<PatternValue>> map =
                    player.getProfile().isLegitimate() ? this.available : this.hidden;
            this.rawCache(map, player, value, 0);

            if (this.generalization) {
                for (int generalization : this.generalizations) {
                    this.rawCache(
                            map,
                            player,
                            new PatternValue(AlgebraUtils.cut(number.doubleValue(), generalization), value.time),
                            generalization
                    );
                }
            }
        }
    }

    // Separator

    public void show(SpartanPlayer player) {
        if (!hidden.isEmpty()) {

        }
    }

    public void hide(SpartanPlayer player) {
        if (!available.isEmpty()) {

        }
    }

    private int hash(UUID uuid, int generalization) {
        return (uuid.hashCode() * SpartanBukkit.hashCodeMultiplier) + generalization;
    }


    // Separator

    public Collection<PatternValue> get() {
        return this.available.get(0);
    }

    public Collection<PatternValue> get(int generalization) {
        return this.available.get(generalization);
    }
}
