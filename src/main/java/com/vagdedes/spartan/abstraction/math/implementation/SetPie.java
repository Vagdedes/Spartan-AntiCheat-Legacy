package com.vagdedes.spartan.abstraction.math.implementation;

import com.vagdedes.spartan.abstraction.math.AbstractKeyPie;

import java.util.*;

public class SetPie implements AbstractKeyPie { // todo optimize distance with total and map with double distance and integer count

    private final Map<Float, Set<Integer>> map;
    private float total;

    public SetPie() {
        this(-1);
    }

    public SetPie(int capacity) {
        this.map = capacity >= 0 ? new HashMap<>(capacity) : new HashMap<>();
        this.total = 0.0f;
    }

    public SetPie(int capacity, float loadFactor) {
        this.map = capacity >= 0 ? new HashMap<>(capacity, loadFactor) : new HashMap<>();
        this.total = 0.0f;
    }

    @Override
    public void clear() {
        this.map.clear();
        this.total = 0.0f;
    }

    @Override
    public Float removeLeastSignificant() {
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
        this.total -= min;
        return key;
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
        return (int) this.total;
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

    // Separator

    @Override
    public double getContribution(Number number, double defaultValue) {
        return 1.0 - (1.0 / (this.total + 1.0));
    }

    @Override
    public double getContribution(Number number, int ignoreHash, double defaultValue) {
        Set<Integer> set = this.map.get(number.floatValue());

        if (set != null) {
            return 1.0 - (1.0 / (this.total + (set.contains(ignoreHash) ? -1.0 : 0.0) + 1.0));
        } else {
            return defaultValue;
        }
    }

    @Override
    public double getRatio(Number number, double defaultValue) {
        Set<Integer> set = this.map.get(number.floatValue());
        return set != null ? set.size() / this.total : defaultValue;
    }

    @Override
    public double getDistance(Number number, double defaultValue) {
        if (this.map.size() > 1) {
            Set<Integer> selfSet = this.map.get(number.floatValue());

            if (selfSet != null) {
                double average = 0.0;
                Set<Double> rank = new TreeSet<>();
                Collection<Set<Integer>> values = this.map.values();

                for (Set<Integer> set : values) {
                    average += set.size();
                }
                average /= this.map.size();

                for (Set<Integer> set : values) {
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
                double average = 0.0;
                Map<Double, Integer> rank = new TreeMap<>();
                Collection<Set<Integer>> values = this.map.values();

                for (Set<Integer> set : values) {
                    average += set.size();
                }
                average /= this.map.size();

                for (Set<Integer> set : values) {
                    double distance = absolute ? Math.abs(set.size() - average) : set.size() - average;
                    rank.put(distance, rank.getOrDefault(distance, 0) + 1);
                }
                average = absolute ? Math.abs(selfSet.size() - average) : selfSet.size() - average;
                int position = 0;

                for (Map.Entry<Double, Integer> entry : rank.entrySet()) {
                    if (entry.getKey() == average) {
                        return position / (double) rank.size();
                    } else {
                        position += entry.getValue();
                    }
                }
            }
        }
        return defaultValue;
    }

}

