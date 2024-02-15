package com.vagdedes.spartan.objects.profiling;

import com.vagdedes.spartan.objects.replicates.SpartanPlayer;

import java.util.ArrayList;
import java.util.Iterator;
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

    public boolean isFighting() {
        int size = fights.size();

        if (size > 0) {
            Iterator<PlayerFight> iterator = fights.iterator();

            while (iterator.hasNext()) {
                PlayerFight fight1 = iterator.next();

                if (fight1.expired()) {
                    iterator.remove();
                    removeFightCopy(fight1);
                    size--;
                }
            }
            return size > 0;
        } else {
            return false;
        }
    }

    public List<PlayerFight> getFights() {
        return new ArrayList<>(fights);
    }

    public PlayerFight getFight(SpartanPlayer opponent) {
        if (!fights.isEmpty()) {
            Iterator<PlayerFight> iterator = fights.iterator();

            while (iterator.hasNext()) {
                PlayerFight fight = iterator.next();

                if (fight.expired()) {
                    iterator.remove();
                    removeFightCopy(fight);
                } else if (fight.opponent1.getPlayer().equals(opponent)
                        || fight.opponent2.getPlayer().equals(opponent)) {
                    return fight;
                }
            }
        }
        return new PlayerFight(new PlayerOpponent(profile.getSpartanPlayer()), new PlayerOpponent(opponent));
    }

    public void track() {
        if (!fights.isEmpty()) {
            Iterator<PlayerFight> iterator = fights.iterator();

            while (iterator.hasNext()) {
                PlayerFight fight = iterator.next();

                if (fight.expired()) {
                    iterator.remove();
                    removeFightCopy(fight);
                }
            }
        }
    }

    private void removeFightCopy(PlayerFight fight) {
        fight.getOpponent(profile.getSpartanPlayer())[1].getPlayer()
                .getProfile().getCombat().fights.remove(fight);
    }
}
