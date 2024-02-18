package com.vagdedes.spartan.objects.profiling;

import com.vagdedes.spartan.objects.replicates.SpartanPlayer;

import java.util.*;

public class PlayerCombat {

    // Separator

    private final PlayerProfile profile;

    // Use of memory based lists to help save performance when iterating in the object (no advantage when iterating globally)
    final Collection<PlayerFight> fights;

    PlayerCombat(PlayerProfile profile) {
        this.profile = profile;
        this.fights = Collections.synchronizedList(new ArrayList<>());
    }

    public List<PlayerFight> getFights() {
        synchronized (fights) {
            return new ArrayList<>(fights);
        }
    }

    public boolean isFighting() {
        int size = fights.size();

        if (size > 0) {
            synchronized (fights) {
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
            }
        } else {
            return false;
        }
    }

    public boolean isActivelyFighting(long hit, long damage) { // todo
        return false;
    }

    public boolean isActivelyFighting(boolean hit, boolean damage) {
        return isActivelyFighting(hit ? 2_500L : -1L, damage ? 2_500L : -1L);
    }

    public PlayerFight getFight(SpartanPlayer opponent) {
        if (!fights.isEmpty()) {
            synchronized (fights) {
                Iterator<PlayerFight> iterator = fights.iterator();

                while (iterator.hasNext()) {
                    PlayerFight fight = iterator.next();

                    if (fight.expired()) {
                        iterator.remove();
                        removeFightCopy(fight);
                    } else if (fight.opponent1.player.equals(opponent)
                            || fight.opponent2.player.equals(opponent)) {
                        return fight;
                    }
                }
            }
        }
        return new PlayerFight(new PlayerOpponent(profile.getSpartanPlayer()), new PlayerOpponent(opponent));
    }

    public void track() {
        if (!fights.isEmpty()) {
            synchronized (fights) {
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
    }

    void removeFight(PlayerFight fight) {
        synchronized (fights) {
            fights.remove(fight);
        }
    }

    private void removeFightCopy(PlayerFight fight) {
        PlayerCombat combat = fight.getOpponent(profile.getSpartanPlayer())[1].player.getProfile().getCombat();

        synchronized (combat.fights) {
            combat.fights.remove(fight);
        }
    }
}
