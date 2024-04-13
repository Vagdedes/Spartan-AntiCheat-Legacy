package com.vagdedes.spartan.abstraction.math.implementation;

import com.vagdedes.spartan.abstraction.math.AbstractKeyValueRank;

import java.util.*;

public class MapRank implements AbstractKeyValueRank {

    private final Map<Float, Map<Integer, Short>> map;
    private final Map<Integer, Integer> rank;
    private final Map<Short, Integer> specificRank;
    private float total, specificTotal;

    public MapRank() {
        this(-1);
    }

    public MapRank(int capacity) {
        this.map = capacity >= 0 ? new HashMap<>(capacity) : new HashMap<>();
        this.rank = new TreeMap<>();
        this.specificRank = new TreeMap<>();
        this.total = 0.0f;
        this.specificTotal = 0.0f;
    }

    public MapRank(int capacity, float loadFactor) {
        this.map = capacity >= 0 ? new HashMap<>(capacity, loadFactor) : new HashMap<>();
        this.rank = new TreeMap<>();
        this.specificRank = new TreeMap<>();
        this.total = 0.0f;
        this.specificTotal = 0.0f;
    }

    @Override
    public void clear() {
        this.map.clear();
        this.rank.clear();
        this.specificRank.clear();
        this.total = 0.0f;
        this.specificTotal = 0.0f;
    }

    @Override
    public Float removeLeastSignificant() {
        if (this.map.isEmpty()) {
            return null;
        } else {
            int min = Integer.MAX_VALUE;
            Float key = null;

            for (Map.Entry<Float, Map<Integer, Short>> entry : this.map.entrySet()) {
                if (entry.getValue().size() < min) {
                    min = entry.getValue().size();
                    key = entry.getKey();

                    if (min == 1) {
                        int specific = 0;

                        for (short count : entry.getValue().values()) {
                            specific += count;
                        }
                        this.specificTotal -= specific;
                        break;
                    }
                }
            }
            this.map.remove(key);
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
        Map<Integer, Short> map = this.map.get(number.floatValue());

        if (map != null) {
            Short value = map.get(hash);

            if (value == null) {
                this.decreaseRank(map.size());
                map.put(hash, (short) 1);
                this.increaseRank(map.size());
                this.total++;
                this.specificTotal++;
                this.increaseSpecificRank((short) 1);
            } else if (value < 32767) {
                short newValue = (short) (value + 1);
                map.put(hash, newValue);
                this.decreaseSpecificRank(value);
                this.increaseSpecificRank(newValue);
                this.total++;
            }
        } else {
            map = new HashMap<>();
            map.put(hash, (short) 1);
            this.map.put(number.floatValue(), map);
            this.total++;
            this.specificTotal++;
            this.increaseRank(1);
            this.increaseSpecificRank((short) 1);
        }
    }

    public boolean remove(Number number, int hash) {
        Map<Integer, Short> map = this.map.get(number.floatValue());

        if (map != null) {
            Short value = map.get(hash);

            if (value != null) {
                this.specificTotal--;

                if (value == 1) {
                    this.decreaseRank(map.size());
                    map.remove(hash);

                    if (map.isEmpty()) {
                        this.map.remove(number.floatValue());
                    } else {
                        this.increaseRank(map.size());
                    }
                    this.total--;
                } else {
                    short newValue = (short) (value - 1);
                    map.put(hash, newValue);
                    this.decreaseSpecificRank(value);
                    this.increaseRank(newValue);
                }
                return true;
            } else {
                return false;
            }
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
        Map<Integer, Short> map = this.map.get(number.floatValue());

        if (map != null) {
            return 1.0 - (1.0 / (this.total + (map.containsKey(ignoreHash) ? -1.0 : 0.0) + 1.0));
        } else {
            return defaultValue;
        }
    }

    @Override
    public double getRatio(Number number, double defaultValue) {
        Map<Integer, Short> map = this.map.get(number.floatValue());
        return map != null ? map.size() / this.total : defaultValue;
    }

    @Override
    public double getDistance(Number number, double defaultValue) {
        if (this.map.size() > 1) {
            Map<Integer, Short> selfMap = this.map.get(number.floatValue());

            if (selfMap != null) {
                double average = this.total / (double) this.map.size();
                Set<Double> rank = new TreeSet<>();

                for (Map<Integer, Short> map : this.map.values()) {
                    rank.add(Math.abs(map.size() - average));
                }
                average = Math.abs(selfMap.size() - average);
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
            Map<Integer, Short> selfMap = this.map.get(number.floatValue());

            if (selfMap != null) {
                double average = this.total / (double) this.map.size();
                Map<Double, Integer> rank = new HashMap<>();

                for (Map<Integer, Short> map : this.map.values()) {
                    double distance = absolute ? Math.abs(map.size() - average) : map.size() - average;
                    rank.put(distance, rank.getOrDefault(distance, 0) + 1);
                }
                average = absolute ? Math.abs(selfMap.size() - average) : selfMap.size() - average;
                return rank.get(average) / (double) rank.size();
            }
        }
        return defaultValue;
    }

    @Override
    public double getPosition(Number number, double defaultValue) {
        if (this.map.size() > 1) {
            Iterator<Map.Entry<Integer, Integer>> iterator = rank.entrySet().iterator();
            double position = 0.0;
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

    @Override
    public double getSpecificContribution(Number number, double defaultValue) {
        return 1.0 - (1.0 / (this.specificTotal + 1.0));
    }

    @Override
    public double getSpecificContribution(Number number, int ignoreHash, double defaultValue) {
        Map<Integer, Short> map = this.map.get(number.floatValue());

        if (map != null) {
            Short count = map.get(ignoreHash);
            return 1.0 - (1.0 / (this.specificTotal + (count != null ? -count : 0.0) + 1.0));
        } else {
            return defaultValue;
        }
    }

    @Override
    public double getSpecificRatio(Number number, int hash, double defaultValue) {
        Map<Integer, Short> map = this.map.get(number.floatValue());

        if (map != null) {
            Short count = map.get(hash);
            return count != null ? count / this.specificTotal : defaultValue;
        } else {
            return defaultValue;
        }
    }

    @Override
    public double getSpecificDistance(Number number, int hash, double defaultValue) {
        if (!this.map.isEmpty()) {
            Map<Integer, Short> selfMap = this.map.get(number.floatValue());

            if (selfMap != null && selfMap.size() > 1) {
                Short selfCount = selfMap.get(hash);

                if (selfCount != null) {
                    double average = this.specificTotal / (double) selfMap.size();
                    Set<Double> rank = new TreeSet<>();

                    for (short count : selfMap.values()) {
                        rank.add(Math.abs(count - average));
                    }
                    average = Math.abs(selfCount - average);
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
        }
        return defaultValue;
    }

    @Override
    public double getSpecificWaveProbability(Number number, int hash, boolean absolute, double defaultValue) {
        if (!this.map.isEmpty()) {
            Map<Integer, Short> selfMap = this.map.get(number.floatValue());

            if (selfMap != null && selfMap.size() > 1) {
                Short selfCount = selfMap.get(hash);

                if (selfCount != null) {
                    double average = this.specificTotal / (double) selfMap.size();
                    Map<Double, Integer> rank = new HashMap<>();

                    for (short count : selfMap.values()) {
                        double distance = absolute ? Math.abs(count - average) : count - average;
                        rank.put(distance, rank.getOrDefault(distance, 0) + 1);
                    }
                    average = absolute ? Math.abs(selfCount - average) : selfCount - average;
                    return rank.get(average) / (double) rank.size();
                }
            }
        }
        return defaultValue;
    }

    @Override
    public double getSpecificPosition(Number number, int hash, double defaultValue) {
        if (!this.map.isEmpty()) {
            Map<Integer, Short> selfMap = this.map.get(number.floatValue());

            if (selfMap != null && selfMap.size() > 1) {
                Short selfCount = selfMap.get(hash);

                if (selfCount != null) {
                    Iterator<Map.Entry<Short, Integer>> iterator = specificRank.entrySet().iterator();
                    double position = 0.0;

                    while (iterator.hasNext()) {
                        Map.Entry<Short, Integer> next = iterator.next();

                        if (next.getKey().equals(selfCount)) {
                            if (next.getValue() > 1) {
                                return (position + (next.getValue() / 2.0)) / this.specificTotal;
                            } else {
                                return (position + 1.0) / this.specificTotal;
                            }
                        } else {
                            position += next.getValue();
                        }
                    }
                }
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

    private void increaseSpecificRank(short count) {
        this.specificRank.put(count, this.specificRank.getOrDefault(count, 0) + 1);
    }

    private void decreaseSpecificRank(short count) {
        this.specificRank.put(count, this.specificRank.get(count) - 1);
    }

}

