package com.vagdedes.spartan.abstraction.math.implementation;

import com.vagdedes.spartan.abstraction.math.AbstractRank;

import java.util.*;

public class HashRank implements AbstractRank {

    private final Map<Float, Set<Integer>> map;
    private float total;

    public HashRank() {
        this.map = new HashMap<>();
        this.total = 0.0f;
    }

    public HashRank(int capacity) {
        this.map = capacity >= 0 ? new HashMap<>(capacity) : new HashMap<>();
        this.total = 0.0f;
    }

    public HashRank(int capacity, float loadFactor) {
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

    public void remove(HashRank rank) {
        for (Map.Entry<Float, Set<Integer>> entry : rank.map.entrySet()) {
            Set<Integer> set = this.map.get(entry.getKey());

            if (set != null) {
                set.removeAll(entry.getValue());

                if (set.isEmpty()) {
                    this.map.remove(entry.getKey());
                }
                this.total -= entry.getValue().size();
            }
        }
    }

    public void delete(Number number) {
        Set<Integer> set = this.map.get(number.floatValue());

        if (set != null) {
            this.map.remove(number.floatValue());
            this.total -= set.size();
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
    public double getPosition(Number number, double defaultValue) {
        Iterator<Map.Entry<Float, Set<Integer>>> iterator = this.map.entrySet().iterator();
        double position = 0.0;
        float find = number.floatValue();

        while (iterator.hasNext()) {
            Map.Entry<Float, Set<Integer>> next = iterator.next();

            if (next.getKey() == find) {
                if (next.getValue().size() > 1) {
                    return (position + (next.getValue().size() / 2.0)) / this.total;
                } else {
                    return (position + 1.0) / this.total;
                }
            } else if (next.getKey() > find) {
                return position / this.total;
            }
            position += next.getValue().size();
        }
        return defaultValue;
    }

    @Override
    public double getPosition(Number number) {
        return this.getPosition(number, 1.0);
    }

    // Separator

    @Override
    public double getRatio(Number number, double defaultValue) {
        Set<Integer> set = this.map.get(number.floatValue());
        return set != null ? set.size() / this.total : defaultValue;
    }

    @Override
    public double getRatio(Number number) {
        return this.getRatio(number, 0.0);
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
        return this.map.isEmpty();
    }

    @Override
    public int getParts() {
        return this.map.size();
    }

    @Override
    public int getTotal() {
        return (int) this.total;
    }
}
