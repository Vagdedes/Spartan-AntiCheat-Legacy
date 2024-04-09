package com.vagdedes.spartan.utils.math;

import java.util.*;

public class HashPie {

    private final Map<Float, Set<Integer>> map;
    private float total;

    public HashPie() {
        this(-1);
    }

    public HashPie(int capacity) {
        this.map = capacity >= 0 ? new HashMap<>(capacity) : new HashMap<>();
        this.total = 0.0f;
    }

    public HashPie(int capacity, float loadFactor) {
        this.map = capacity >= 0 ? new HashMap<>(capacity, loadFactor) : new HashMap<>();
        this.total = 0.0f;
    }

    public HashPie clear() {
        this.map.clear();
        this.total = 0.0f;
        return this;
    }

    // Separator

    public HashPie addMultiple(Collection<? extends Number> numbers, int hash) {
        for (Number number : numbers) {
            this.add(number, hash);
        }
        return this;
    }

    // Separator

    public HashPie add(Number number, int hash) {
        Set<Integer> set = this.map.get(number.floatValue());

        if (set != null) {
            if (set.add(hash)) {
                this.total++;
            }
        } else {
            set = new HashSet<>();
            set.add(hash);
            this.map.put(number.floatValue(), set);
            this.total++;
        }
        return this;
    }

    public HashPie add(HashPie pie) {
        for (Map.Entry<Float, Set<Integer>> entry : pie.map.entrySet()) {
            Set<Integer> set = this.map.get(entry.getKey());

            if (set != null) {
                for (int hash : entry.getValue()) {
                    if (set.add(hash)) {
                        this.total++;
                    }
                }
            } else {
                this.map.put(entry.getKey(), new HashSet<>(entry.getValue()));
                this.total += entry.getValue().size();
            }
        }
        return this;
    }

    // Separator

    public boolean remove(Number number, int hash) {
        Set<Integer> set = this.map.get(number.floatValue());

        if (set != null && set.remove(hash)) {
            if (set.isEmpty()) {
                this.map.remove(number.floatValue());
            }
            this.total--;
            return true;
        } else {
            return false;
        }
    }

    public HashPie remove(HashPie pie) {
        for (Map.Entry<Float, Set<Integer>> entry : pie.map.entrySet()) {
            Set<Integer> set = this.map.get(entry.getKey());

            if (set != null) {
                set.removeAll(entry.getValue());

                if (set.isEmpty()) {
                    this.map.remove(entry.getKey());
                }
                this.total -= entry.getValue().size();
            }
        }
        return this;
    }

    public Float removeLeastSignificant() {
        float min = Float.MAX_VALUE;
        Float key = null;

        for (Map.Entry<Float, Set<Integer>> entry : this.map.entrySet()) {
            if (entry.getValue().size() < min) {
                min = entry.getValue().size();
                key = entry.getKey();

                if (min == 1) {
                    break;
                }
            }
        }
        this.map.remove(key);
        return key;
    }

    // Separator

    public double getRatio(Number number) {
        return this.getRatio(number, 0.0);
    }

    public double getRatio(Number number, double defaultValue) {
        Set<Integer> set = this.map.get(number.floatValue());
        return set != null ? set.size() / this.total : defaultValue;
    }

    // Separator

    public int getCount(Number number) {
        return this.getCount(number, 0);
    }

    public int getCount(Number number, int defaultValue) {
        Set<Integer> set = this.map.get(number.floatValue());
        return set == null ? defaultValue : set.size();
    }

    // Separator

    public boolean isEmpty() {
        return this.total == 0;
    }

    public int getSlices() {
        return this.map.size();
    }

    public int getTotal() {
        return (int) this.total;
    }

    public float getRawTotal() {
        return this.total;
    }
}

