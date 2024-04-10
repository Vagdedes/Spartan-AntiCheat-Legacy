package com.vagdedes.spartan.abstraction.math.implementation;

import com.vagdedes.spartan.abstraction.math.AbstractPie;

import java.util.*;

public class HashPie implements AbstractPie {

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

    @Override
    public void clear() {
        this.map.clear();
        this.total = 0.0f;
    }

    // Separator

    public void addMultiple(Collection<? extends Number> numbers, int hash) {
        for (Number number : numbers) {
            this.add(number, hash);
        }
    }

    // Separator

    public void add(Number number, int hash) {
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

    @Override
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

    @Override
    public double getRatio(Number number) {
        return this.getRatio(number, 0.0);
    }

    @Override
    public double getRatio(Number number, double defaultValue) {
        Set<Integer> set = this.map.get(number.floatValue());
        return set != null ? set.size() / this.total : defaultValue;
    }

    // Separator

    @Override
    public int getCount(Number number) {
        return this.getCount(number, 0);
    }

    @Override
    public int getCount(Number number, int defaultValue) {
        Set<Integer> set = this.map.get(number.floatValue());
        return set == null ? defaultValue : set.size();
    }

    public Set<Integer> getSet(Number number) {
        Set<Integer> set = this.map.get(number.floatValue());
        return set == null ? new HashSet<>(0) : new HashSet<>(set);
    }

    // Separator

    @Override
    public boolean isEmpty() {
        return this.total == 0;
    }

    @Override
    public int getSlices() {
        return this.map.size();
    }

    @Override
    public int getTotal() {
        return (int) this.total;
    }
}

