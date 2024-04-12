package com.vagdedes.spartan.abstraction.math.implementation;

import com.vagdedes.spartan.abstraction.math.AbstractKeyValueRank;

import java.util.*;

public class MapRank implements AbstractKeyValueRank {

    private final Map<Float, Map<Integer, Short>> map;
    private final Map<Integer, Integer> rank, uniqueRank;
    private float total, specificTotal;

    public MapRank() {
        this(-1);
    }

    public MapRank(int capacity) {
        this.map = capacity >= 0 ? new HashMap<>(capacity) : new HashMap<>();
        this.rank = new TreeMap<>();
        this.uniqueRank = new TreeMap<>();
        this.total = 0.0f;
        this.specificTotal = 0.0f;
    }

    public MapRank(int capacity, float loadFactor) {
        this.map = capacity >= 0 ? new HashMap<>(capacity, loadFactor) : new HashMap<>();
        this.rank = new TreeMap<>();
        this.uniqueRank = new TreeMap<>();
        this.total = 0.0f;
        this.specificTotal = 0.0f;
    }

    @Override
    public void clear() {
        this.map.clear();
        this.rank.clear();
        this.uniqueRank.clear();
        this.total = 0.0f;
        this.specificTotal = 0.0f;
    }

    @Override
    public Float removeLeastSignificant() {
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
                map.put(hash, (short) 1);
                this.total++;
                this.specificTotal++;
                this.decreaseRank(map.size() - 1);
                this.increaseRank(map.size());
                this.decreaseUniqueRank(map.size() - 1);
                this.increaseUniqueRank(map.size());
            } else if (value < 32767) {
                map.put(hash, (short) (value + 1));
                this.total++;
                this.decreaseRank(value);
                this.increaseRank(value + 1);
            }
        } else {
            map = new HashMap<>();
            map.put(hash, (short) 1);
            this.map.put(number.floatValue(), map);
            this.total++;
            this.specificTotal++;
            this.increaseRank(1);
            this.increaseUniqueRank(1);
        }
    }

    public boolean remove(Number number, int hash) {
        Map<Integer, Short> map = this.map.get(number.floatValue());

        if (map != null) {
            Short value = map.get(hash);

            if (value != null) {
                if (value == 1) {
                    map.remove(hash);

                    if (map.isEmpty()) {
                        this.map.remove(number.floatValue());
                    }
                    this.total--;
                    this.specificTotal--;
                } else {
                    map.put(hash, (short) (value - 1));
                    this.total--;
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
                double average = 0.0;
                Map<Double, Integer> rank = new TreeMap<>();
                Collection<Map<Integer, Short>> values = this.map.values();

                for (Map<Integer, Short> map : values) {
                    average += map.size();
                }
                average /= this.map.size();

                for (Map<Integer, Short> map : values) {
                    double distance = Math.abs(map.size() - average);
                    rank.put(distance, rank.getOrDefault(distance, 0) + 1);
                }
                average = Math.abs(selfMap.size() - average);
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
        return 0;
    }

    @Override
    public double getSpecificPosition(Number number, int hash, double defaultValue) {
        return 0;
    }

    // Separator

    private void increaseRank(int size) {
        this.rank.put(size, this.rank.getOrDefault(size, 0) + 1);
    }

    private void decreaseRank(int size) {
        this.rank.put(size, this.rank.get(size) - 1);
    }

    private void increaseUniqueRank(int size) {
        this.uniqueRank.put(size, this.uniqueRank.getOrDefault(size, 0) + 1);
    }

    private void decreaseUniqueRank(int size) {
        this.uniqueRank.put(size, this.uniqueRank.get(size) - 1);
    }
}

