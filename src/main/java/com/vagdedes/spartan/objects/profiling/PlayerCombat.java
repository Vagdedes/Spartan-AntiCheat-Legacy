package com.vagdedes.spartan.objects.profiling;

import com.vagdedes.spartan.objects.replicates.SpartanPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PlayerCombat {

    // Separator

    private final PlayerProfile profile;

    // Use of memory based lists to help save performance when iterating in the object (no advantage when iterating globally)
    private final List<PlayerFight> fights = new CopyOnWriteArrayList<>();

    PlayerCombat(PlayerProfile profile) {
        this.profile = profile;
    }

    // Separator

    public void removeFight(PlayerFight fight) {
        fights.remove(fight);
    }

    public void createFight(PlayerFight fight) {
        fights.add(fight);
    }

    public boolean isFighting() {
        return !fights.isEmpty();
    }

    // Separator

    public List<PlayerFight> getFights() {
        return new ArrayList<>(fights);
    }

    // Separator

    public PlayerFight getFight(SpartanPlayer opponent) {
        List<PlayerFight> fights = getFights();

        if (!fights.isEmpty()) {
            for (PlayerFight fight : fights) {
                if (fight.getOpponent1().getPlayer().equals(opponent)
                        || fight.getOpponent2().getPlayer().equals(opponent)) {
                    return fight;
                }
            }
        }
        return new PlayerFight(new PlayerOpponent(profile.getSpartanPlayer()), new PlayerOpponent(opponent));
    }

    // Separator

    public void track() {
        if (!fights.isEmpty()) {
            for (PlayerFight fight : fights) {
                fight.track();
            }
        }
    }
}
