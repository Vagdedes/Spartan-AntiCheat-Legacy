package com.vagdedes.spartan.abstraction.math.implementation;

import com.vagdedes.spartan.abstraction.math.AbstractPie;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MapPie implements AbstractPie {

    private final Map<Float, Map<Integer, Short>> map;
    private float total, uniqueTotal;

    public MapPie() {
        this(-1);
    }

    public MapPie(int capacity) {
        this.map = capacity >= 0 ? new HashMap<>(capacity) : new HashMap<>();
        this.total = 0.0f;
        this.uniqueTotal = 0.0f;
    }

    public MapPie(int capacity, float loadFactor) {
        this.map = capacity >= 0 ? new HashMap<>(capacity, loadFactor) : new HashMap<>();
        this.total = 0.0f;
        this.uniqueTotal = 0.0f;
    }

    @Override
    public void clear() {
        this.map.clear();
        this.total = 0.0f;
        this.uniqueTotal = 0.0f;
    }

    // Separator

    public void addMultiple(Collection<? extends Number> numbers, int hash) {
        for (Number number : numbers) {
            this.add(number, hash);
        }
    }

    // Separator

    public void add(Number number, int hash) {
        Map<Integer, Short> map = this.map.get(number.floatValue());

        if (map != null) {
            Short value = map.get(hash);

            if (value == null) {
                map.put(hash, (short) 1);
                this.total++;
                this.uniqueTotal++;
            } else if (value < 32767) {
                map.put(hash, (short) (value + 1));
                this.total++;
            }
        } else {
            map = new HashMap<>();
            map.put(hash, (short) 1);
            this.map.put(number.floatValue(), map);
            this.total++;
            this.uniqueTotal++;
        }
    }

    // Separator

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
                    this.uniqueTotal--;
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

    @Override
    public Float removeLeastSignificant() {
        int min = Integer.MAX_VALUE;
        Float key = null;

        for (Map.Entry<Float, Map<Integer, Short>> entry : this.map.entrySet()) {
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

    // Separator

    @Override
    public double getRatio(Number number, double defaultValue) {
        Map<Integer, Short> map = this.map.get(number.floatValue());

        if (map != null) {
            int count = 0;

            for (short value : map.values()) {
                count += value;
            }
            return count / this.total;
        } else {
            return defaultValue;
        }
    }

    public double getRatio(Number number, int ignoreHash, double defaultValue) {
        Map<Integer, Short> map = this.map.get(number.floatValue());

        if (map != null) {
            int count = 0;

            for (Map.Entry<Integer, Short> entry : map.entrySet()) {
                if (entry.getKey() != ignoreHash) {
                    count += entry.getValue();
                }
            }
            return count / this.total;
        } else {
            return defaultValue;
        }
    }

    public double getUniqueRatio(Number number, double defaultValue) {
        Map<Integer, Short> map = this.map.get(number.floatValue());
        return map != null ? map.size() / this.uniqueTotal : defaultValue;
    }

    public double getUniqueRatio(Number number, int ignoreHash, double defaultValue) {
        Map<Integer, Short> map = this.map.get(number.floatValue());
        return map != null
                ? (map.containsKey(ignoreHash) ? map.size() - 1 : map.size()) / this.uniqueTotal
                : defaultValue;
    }

    // Separator

    @Override
    public int getCount(Number number, int defaultValue) {
        Map<Integer, Short> map = this.map.get(number.floatValue());

        if (map != null) {
            int count = 0;

            for (short value : map.values()) {
                count += value;
            }
            return count;
        } else {
            return defaultValue;
        }
    }

    public int getUniqueCount(Number number) {
        return this.getUniqueCount(number, 0);
    }

    public int getUniqueCount(Number number, int defaultValue) {
        Map<Integer, Short> map = this.map.get(number.floatValue());
        return map == null ? defaultValue : map.size();
    }

    public Map<Integer, Short> getMap(Number number) {
        Map<Integer, Short> map = this.map.get(number.floatValue());
        return map == null ? new HashMap<>(0) : new HashMap<>(map);
    }

    // Separator

    @Override
    public boolean isEmpty() {
        return this.total == 0;
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

