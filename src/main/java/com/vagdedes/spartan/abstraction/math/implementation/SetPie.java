package com.vagdedes.spartan.abstraction.math.implementation;

import com.vagdedes.spartan.abstraction.math.AbstractKeyPie;

import java.util.*;

public class SetPie implements AbstractKeyPie {

    private final Map<Float, Set<Integer>> map;

    public SetPie() {
        this(-1);
    }

    public SetPie(int capacity) {
        this.map = capacity >= 0 ? new HashMap<>(capacity) : new HashMap<>();
    }

    public SetPie(int capacity, float loadFactor) {
        this.map = capacity >= 0 ? new HashMap<>(capacity, loadFactor) : new HashMap<>();
    }

    @Override
    public void clear() {
        this.map.clear();
    }

    @Override
    public Float removeLeastSignificant() {
        if (this.map.isEmpty()) {
            return null;
        } else {
            int min = Integer.MAX_VALUE;
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
    }

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
        return this.map.size();
    }

    // Separator

    public void addMultiple(Collection<? extends Number> numbers, int hash) {
        for (Number number : numbers) {
            this.add(number, hash);
        }
    }

    public void add(Number number, int hash) {
        Set<Integer> set = this.map.get(number.floatValue());

        if (set != null) {
            set.add(hash);
        } else {
            set = new HashSet<>();
            set.add(hash);
            this.map.put(number.floatValue(), set);
        }
    }

    public boolean remove(Number number, int hash) {
        Set<Integer> set = this.map.get(number.floatValue());

        if (set != null && set.remove(hash)) {
            if (set.isEmpty()) {
                this.map.remove(number.floatValue());
            }
            return true;
        } else {
            return false;
        }
    }

    // Separator

    @Override
    public double getContribution(Number number, double defaultValue) {
        return 1.0 - (1.0 / (this.map.size() + 1.0));
    }

    @Override
    public double getContribution(Number number, int ignoreHash, double defaultValue) {
        Set<Integer> set = this.map.get(number.floatValue());

        if (set != null) {
            return 1.0 - (1.0 / (this.map.size() + (set.contains(ignoreHash) ? -1.0 : 0.0) + 1.0));
        } else {
            return defaultValue;
        }
    }

    @Override
    public double getRatio(Number number, double defaultValue) {
        Set<Integer> set = this.map.get(number.floatValue());
        return set != null ? set.size() / (double) this.map.size() : defaultValue;
    }

    @Override
    public double getDistance(Number number, double defaultValue) {
        if (this.map.size() > 1) {
            Set<Integer> selfSet = this.map.get(number.floatValue());

            if (selfSet != null) {
                double average = this.total / (double) this.map.size();
                Set<Double> rank = new TreeSet<>();

                for (Set<Integer> set : this.map.values()) {
                    rank.add(Math.abs(set.size() - average));
                }
                average = Math.abs(selfSet.size() - average);
                int position = 0;

                for (double distance : rank) {
                    if (distance == average) {
                        return position / (double) rank.size();
                    } else {
                        position++;
                    }
                }
            }
        }
        return defaultValue;
    }

    @Override
    public double getWaveProbability(Number number, boolean absolute, double defaultValue) {
        if (this.map.size() > 1) {
            Set<Integer> selfSet = this.map.get(number.floatValue());

            if (selfSet != null) {
                double average = this.total / (double) this.map.size();
                Map<Double, Integer> rank = new HashMap<>();

                for (Set<Integer> set : this.map.values()) {
                    double distance = absolute ? Math.abs(set.size() - average) : set.size() - average;
                    rank.put(distance, rank.getOrDefault(distance, 0) + 1);
                }
                average = absolute ? Math.abs(selfSet.size() - average) : selfSet.size() - average;
                return rank.get(average) / (double) rank.size();
            }
        }
        return defaultValue;
    }

}

