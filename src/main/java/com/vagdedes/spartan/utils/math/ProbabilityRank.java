package com.vagdedes.spartan.utils.math;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class ProbabilityRank {

    private final Map<Double, Integer> map;
    private double total;

    public ProbabilityRank() {
        this.map = new TreeMap<>();
        this.total = 0.0;
    }

    public ProbabilityRank clear() {
        this.map.clear();
        this.total = 0.0;
        return this;
    }

    public ProbabilityRank addMultiple(Collection<? extends Number> numbers) {
        for (Number number : numbers) {
            this.map.put(
                    number.doubleValue(),
                    this.map.getOrDefault(number.doubleValue(), 0) + 1
            );
            this.total++;
        }
        return this;
    }

    public ProbabilityRank addMultiple(Map<? extends Number, Integer> map) {
        for (Map.Entry<? extends Number, Integer> entry : map.entrySet()) {
            this.map.put(
                    entry.getKey().doubleValue(),
                    this.map.getOrDefault(entry.getKey().doubleValue(), 0) + entry.getValue()
            );
            this.total += entry.getValue();
        }
        return this;
    }

    public ProbabilityRank add(Number number) {
        this.map.put(
                number.doubleValue(),
                this.map.getOrDefault(number.doubleValue(), 0) + 1
        );
        this.total++;
        return this;
    }

    public ProbabilityRank remove(Number number) {
        int count = this.map.getOrDefault(number.doubleValue(), 0);

        if (count == 1) {
            this.map.remove(number.doubleValue());
            this.total--;
        } else if (count > 0) {
            this.map.put(number.doubleValue(), count - 1);
            this.total--;
        }
        return this;
    }

    public ProbabilityRank delete(Number number) {
        int count = this.map.getOrDefault(number.doubleValue(), 0);

        if (count > 0) {
            this.map.remove(number.doubleValue());
            this.total -= count;
        }
        return this;
    }

    // Separator

    public double getPosition(Number number, double defaultValue) {
        Iterator<Map.Entry<Double, Integer>> iterator = this.map.entrySet().iterator();
        double position = 0.0, find = number.doubleValue();

        while (iterator.hasNext()) {
            Map.Entry<Double, Integer> next = iterator.next();

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
        return defaultValue;
    }

    public double getPosition(Number number) {
        return this.getPosition(number, 1.0);
    }

    public double getProbability(Number number, double defaultValue) {
        Integer count = this.map.get(number.doubleValue());
        return count != null ? count / this.total : defaultValue;
    }

    public double getProbability(Number number) {
        return this.getProbability(number, 0.0);
    }

    // Separator

    public boolean hasData() {
        return this.total > 0;
    }

    public int getTotal() {
        return (int) this.total;
    }

    public double getRawTotal() {
        return this.total;
    }
}
