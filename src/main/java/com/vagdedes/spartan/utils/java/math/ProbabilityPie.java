package com.vagdedes.spartan.utils.java.math;

import java.util.LinkedHashMap;
import java.util.Map;

public class ProbabilityPie {

    private final Map<Integer, Integer> map;
    private double total;

    public ProbabilityPie() {
        this(-1);
    }

    public ProbabilityPie(int capacity) {
        this.map = capacity >= 0 ? new LinkedHashMap<>(capacity) : new LinkedHashMap<>();
        this.total = 0;
    }

    public void clear() {
        this.map.clear();
        this.total = 0;
    }

    // Separator

    public void create(int hash, int amount) {
        Integer count = this.map.get(hash);

        if (count != null) {
            this.total -= count;
        }
        this.map.put(hash, amount);
        this.total += amount;
    }

    public void add(int hash) {
        this.increase(hash, 1);
    }

    public void increase(int hash, int amount) {
        this.map.put(hash, this.map.getOrDefault(hash, 0) + amount);
        this.total += amount;
    }

    // Separator

    public void delete(int hash) {
        Integer count = this.map.remove(hash);

        if (count != null) {
            this.total -= count;
        }
    }

    public void remove(int hash) {
        this.decrease(hash, 1);
    }

    public void decrease(int hash, int amount) {
        Integer count = this.map.get(hash);

        if (count != null) {
            if (amount >= count) {
                this.map.remove(hash);
                this.total -= count;
            } else {
                this.map.put(hash, count - amount);
                this.total -= amount;
            }
        }
    }

    // Separator

    public double getChance(int hash) {
        return this.getChance(hash, 0.0);
    }

    public double getChance(int hash, double defaultValue) {
        Integer count = this.map.get(hash);
        return count != null ? count / this.total : defaultValue;
    }

    // Separator

    public int getCount(int hash) {
        return getCount(hash, 0);
    }

    public int getCount(int hash, int defaultValue) {
        return this.map.getOrDefault(hash, defaultValue);
    }

    public int getTotal() {
        return (int) this.total;
    }
}
