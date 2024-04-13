package com.vagdedes.spartan.abstraction.math.implementation;

import com.vagdedes.spartan.abstraction.math.AbstractKeyRank;

import java.util.*;

public class SetRank implements AbstractKeyRank {

    private final Map<Float, Set<Integer>> map;
    private final Map<Integer, Integer> rank;
    private float total;

    public SetRank() {
        this(-1);
    }

    public SetRank(int capacity) {
        this.map = capacity >= 0 ? new HashMap<>(capacity) : new HashMap<>();
        this.rank = new TreeMap<>();
        this.total = 0.0f;
    }

    public SetRank(int capacity, float loadFactor) {
        this.map = capacity >= 0 ? new HashMap<>(capacity, loadFactor) : new HashMap<>();
        this.rank = new TreeMap<>();
        this.total = 0.0f;
    }

    @Override
    public void clear() {
        this.map.clear();
        this.rank.clear();
        this.total = 0.0f;
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
            this.decreaseRank(min);
            this.total -= min;
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
                this.decreaseRank(set.size() - 1);
                this.increaseRank(set.size());
            }
        } else {
            set = new HashSet<>();
            set.add(hash);
            this.map.put(number.floatValue(), set);
            this.increaseRank(1);
            this.total++;
        }
    }

    public boolean remove(Number number, int hash) {
        Set<Integer> set = this.map.get(number.floatValue());

        if (set != null && set.remove(hash)) {
            this.decreaseRank(set.size() + 1);

            if (set.isEmpty()) {
                this.map.remove(number.floatValue());
            } else {
                this.increaseRank(set.size());
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
    public double getSlopeProbability(Number number, double defaultValue) {
        if (this.map.size() > 1) {
            Set<Integer> selfSet = this.map.get(number.floatValue());

            if (selfSet != null) {
                double average = this.total / (double) this.map.size();
                Map<Double, Integer> rank = new HashMap<>();

                for (Set<Integer> set : this.map.values()) {
                    double distance = Math.abs(set.size() - average);
                    rank.put(distance, rank.getOrDefault(distance, 0) + 1);
                }
                return rank.get(Math.abs(selfSet.size() - average)) / (double) rank.size();
            }
        }
        return defaultValue;
    }

    @Override
    public double getCurveProbability(Number number, double defaultValue) {
        if (this.map.size() > 1) {
            Set<Integer> selfSet = this.map.get(number.floatValue());

            if (selfSet != null) {
                double average = this.total / (double) this.map.size();
                Map<Double, Integer> rank = new HashMap<>();

                for (Set<Integer> set : this.map.values()) {
                    double distance = set.size() - average;
                    rank.put(distance, rank.getOrDefault(distance, 0) + 1);
                }
                return rank.get(selfSet.size() - average) / (double) rank.size();
            }
        }
        return defaultValue;
    }

    @Override
    public double getPosition(Number number, double defaultValue) {
        if (this.map.size() > 1) {
            Iterator<Map.Entry<Integer, Integer>> iterator = rank.entrySet().iterator();
            int position = 0;
            int find = this.map.get(number.floatValue()).size();

            while (iterator.hasNext()) {
                Map.Entry<Integer, Integer> next = iterator.next();

                if (next.getKey() == find) {
                    if (next.getValue() > 1) {
                        return (position + (next.getValue() / 2.0)) / this.total;
                    } else {
                        return (position + 1.0) / this.total;
                    }
                } else if (next.getKey() > find) {
                    return position / this.total;
                }
                position += next.getValue();
            }
        }
        return defaultValue;
    }

    // Separator

    private void increaseRank(int size) {
        this.rank.put(size, this.rank.getOrDefault(size, 0) + 1);
    }

    private void decreaseRank(int size) {
        this.rank.put(size, this.rank.get(size) - 1);
    }

}
