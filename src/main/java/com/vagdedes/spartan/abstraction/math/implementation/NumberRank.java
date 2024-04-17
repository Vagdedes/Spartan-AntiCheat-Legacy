package com.vagdedes.spartan.abstraction.math.implementation;

import com.vagdedes.spartan.abstraction.math.AbstractRank;

import java.util.*;

public class NumberRank implements AbstractRank {

    private final Map<Integer, Short> map;
    private float total;

    public NumberRank() {
        this.map = new TreeMap<>();
        this.total = 0.0f;
    }

    @Override
    public void clear() {
        this.map.clear();
        this.total = 0.0f;
    }

    @Override
    public Number removeLeastSignificant() {
        if (this.map.isEmpty()) {
            return null;
        } else {
            int min = Integer.MAX_VALUE;
            Integer key = null;

            for (Map.Entry<Integer, Short> entry : this.map.entrySet()) {
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

    @Override
    public int getCount(Number number) {
        return this.map.getOrDefault(number.hashCode(), (short) 0);
    }

    // Separator

    public void addMultiple(Collection<? extends Number> numbers) {
        for (Number number : numbers) {
            this.add(number);
        }
    }

    public void addMultiple(Map<? extends Number, ? extends Number> map) {
        for (Map.Entry<? extends Number, ? extends Number> entry : map.entrySet()) {
            short newCount = (short) Math.min(
                    this.map.getOrDefault(entry.getKey().hashCode(), (short) 0) + entry.getValue().shortValue(),
                    32767
            );
            this.map.put(entry.getKey().hashCode(), newCount);
            this.total += entry.getValue().shortValue();
        }
    }

    public void add(Number number) {
        short current = this.map.getOrDefault(number.hashCode(), (short) 0);

        if (current < 32767) {
            this.map.put(
                    number.hashCode(),
                    (short) (current + 1)
            );
            this.total++;
        }
    }

    public boolean remove(Number number) {
        Short count = this.map.get(number.hashCode());

        if (count != null) {
            this.total--;

            if (count == 1) {
                this.map.remove(number.hashCode());
                return true;
            } else {
                this.map.put(number.hashCode(), (short) (count - 1));
                return false;
            }
        } else {
            return false;
        }
    }

    // Separator

    @Override
    public double getGeneralContribution() {
        return 1.0 / (this.total + 1.0);
    }

    @Override
    public double getUniqueContribution() {
        return 1.0 / (this.map.size() + 1.0);
    }

    @Override
    public double getGeneralContribution(Number ignoreNumber) {
        short count = this.map.getOrDefault(ignoreNumber.hashCode(), (short) 0);
        return 1.0 / (this.total - count + 1.0);
    }

    @Override
    public double getRatio(Number number, double defaultValue) {
        Short count = this.map.get(number.hashCode());
        return count != null ? count / this.total : defaultValue;
    }

    @Override
    public double getDistance(Number number, double defaultValue) {
        if (this.map.size() > 1) {
            Short selfCount = this.map.get(number.hashCode());

            if (selfCount != null) {
                double average = this.total / (double) this.map.size();
                Set<Double> rank = new TreeSet<>();

                for (short count : this.map.values()) {
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
    public double getSlopeProbability(Number number, double defaultValue) {
        if (this.map.size() > 1) {
            Short selfCount = this.map.get(number.hashCode());

            if (selfCount != null) {
                double average = this.total / (double) this.map.size();
                Map<Double, Integer> rank = new HashMap<>();

                for (short count : this.map.values()) {
                    double distance = Math.abs(count - average);
                    rank.put(distance, rank.getOrDefault(distance, 0) + 1);
                }
                return rank.get(Math.abs(selfCount - average)) / (double) rank.size();
            }
        }
        return defaultValue;
    }

    @Override
    public double getCurveProbability(Number number, double defaultValue) {
        if (this.map.size() > 1) {
            Short selfCount = this.map.get(number.hashCode());

            if (selfCount != null) {
                double average = this.total / (double) this.map.size();
                Map<Double, Integer> rank = new HashMap<>();

                for (short count : this.map.values()) {
                    double distance = count - average;
                    rank.put(distance, rank.getOrDefault(distance, 0) + 1);
                }
                return rank.get(selfCount - average) / (double) rank.size();
            }
        }
        return defaultValue;
    }

    @Override
    public double getPosition(Number number, double defaultValue) {
        if (this.map.size() > 1) {
            int position = 0;
            float find = number.hashCode();

            for (Map.Entry<Integer, Short> entry : this.map.entrySet()) {
                if (entry.getKey() == find) {
                    if (entry.getValue() > 1) {
                        return (position + (entry.getValue() / 2.0)) / this.total;
                    } else {
                        return (position + 1.0) / this.total;
                    }
                } else if (entry.getKey() > find) {
                    return position / this.total;
                }
                position += entry.getValue();
            }
        }
        return defaultValue;
    }

}
