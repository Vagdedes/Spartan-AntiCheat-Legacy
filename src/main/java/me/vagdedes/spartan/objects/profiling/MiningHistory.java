package me.vagdedes.spartan.objects.profiling;

import me.vagdedes.spartan.system.Enums;

public class MiningHistory {

    private final Enums.MiningOre ore;
    private int mines, days;

    public MiningHistory(Enums.MiningOre ore, int mines, int days) {
        this.ore = ore;
        this.mines = mines;
        this.days = Math.max(days, 1);
    }

    MiningHistory(Enums.MiningOre ore) {
        this.ore = ore;
        this.mines = 0;
        this.days = 1;
    }

    public Enums.MiningOre getOre() {
        return ore;
    }

    public int getMines() {
        return mines;
    }

    public int increaseMines(int amount) {
        return mines += amount;
    }

    public int getDays() {
        return days;
    }

    public int increaseDays() {
        return days += 1;
    }
}
