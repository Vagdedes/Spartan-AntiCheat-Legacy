package com.vagdedes.spartan.abstraction.math.implementation;

import com.vagdedes.spartan.abstraction.math.AbstractPie;

import java.util.*;

public class NumberPie implements AbstractPie {

    private final Map<Float, Short> map;
    private float total;

    public NumberPie() {
        this(-1);
    }

    public NumberPie(int capacity) {
        this.map = capacity >= 0 ? new HashMap<>(capacity) : new HashMap<>();
        this.total = 0.0f;
    }

    public NumberPie(int capacity, float loadFactor) {
        this.map = capacity >= 0 ? new HashMap<>(capacity, loadFactor) : new HashMap<>();
        this.total = 0.0f;
    }

    @Override
    public void clear() {
        this.map.clear();
        this.total = 0.0f;
    }

    public Float removeLeastSignificant() {
        int min = Integer.MAX_VALUE;
        Float key = null;

        for (Map.Entry<Float, Short> entry : this.map.entrySet()) {
            if (entry.getValue() < min) {
                min = entry.getValue();
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

    public void addMultiple(Collection<? extends Number> numbers) {
        for (Number number : numbers) {
            this.add(number);
        }
    }

    public void addMultiple(Map<? extends Number, Integer> map) {
        for (Map.Entry<? extends Number, Integer> entry : map.entrySet()) {
            this.map.put(
                    entry.getKey().floatValue(),
                    (short) (this.map.getOrDefault(entry.getKey().floatValue(), (short) 0) + entry.getValue())
            );
            this.total += entry.getValue();
        }
    }

    // Separator

    public void add(Number number) {
        short current = this.map.getOrDefault(number.floatValue(), (short) 0);

        if (current < 32767) {
            this.map.put(
                    number.floatValue(),
                    (short) (current + 1)
            );
            this.total += 1;
        }
    }

    public boolean remove(Number number) {
        Short count = this.map.get(number.floatValue());

        if (count != null) {
            if (count == 1) {
                this.map.remove(number.floatValue());
                this.total -= count;
                return true;
            } else {
                this.map.put(number.floatValue(), (short) (count - 1));
                this.total -= 1;
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
    public double getRatio(Number number, double defaultValue) {
        Short count = this.map.get(number.floatValue());
        return count != null ? count / this.total : defaultValue;
    }

    @Override
    public double getDistance(Number number, double defaultValue) {
        if (this.map.size() > 1) {
            Short selfCount = this.map.get(number.floatValue());

            if (selfCount != null) {
                double average = 0.0;
                Set<Double> rank = new TreeSet<>();
                Collection<Short> values = this.map.values();

                for (short count : values) {
                    average += count;
                }
                average /= this.map.size();

                for (short count : values) {
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
        return defaultValue;
    }

    @Override
    public double getWaveProbability(Number number, boolean absolute, double defaultValue) {
        if (this.map.size() > 1) {
            Short selfCount = this.map.get(number.floatValue());

            if (selfCount != null) {
                double average = 0.0;
                Map<Double, Integer> rank = new TreeMap<>();
                Collection<Short> values = this.map.values();

                for (short count : values) {
                    average += count;
                }
                average /= this.map.size();

                for (short count : values) {
                    double distance = absolute ? Math.abs(count - average) : count - average;
                    rank.put(distance, rank.getOrDefault(distance, 0) + 1);
                }
                average = absolute ? Math.abs(selfCount - average) : selfCount - average;
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

