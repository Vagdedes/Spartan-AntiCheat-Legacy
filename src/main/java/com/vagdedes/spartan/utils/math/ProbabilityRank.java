package com.vagdedes.spartan.utils.math;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class ProbabilityRank {

    private final Map<Float, Integer> map;
    private float total;

    public ProbabilityRank() {
        this.map = new TreeMap<>();
        this.total = 0.0f;
    }

    public ProbabilityRank clear() {
        this.map.clear();
        this.total = 0.0f;
        return this;
    }

    public ProbabilityRank addMultiple(Collection<? extends Number> numbers) {
        for (Number number : numbers) {
            this.map.put(
                    number.floatValue(),
                    this.map.getOrDefault(number.floatValue(), 0) + 1
            );
            this.total++;
        }
        return this;
    }

    public ProbabilityRank addMultiple(Map<? extends Number, Integer> map) {
        for (Map.Entry<? extends Number, Integer> entry : map.entrySet()) {
            this.map.put(
                    entry.getKey().floatValue(),
                    this.map.getOrDefault(entry.getKey().floatValue(), 0) + entry.getValue()
            );
            this.total += entry.getValue();
        }
        return this;
    }

    public ProbabilityRank add(Number number) {
        this.map.put(
                number.floatValue(),
                this.map.getOrDefault(number.floatValue(), 0) + 1
        );
        this.total++;
        return this;
    }

    public ProbabilityRank remove(Number number) {
        int count = this.map.getOrDefault(number.floatValue(), 0);

        if (count == 1) {
            this.map.remove(number.floatValue());
            this.total--;
        } else if (count > 0) {
            this.map.put(number.floatValue(), count - 1);
            this.total--;
        }
        return this;
    }

    public ProbabilityRank delete(Number number) {
        int count = this.map.getOrDefault(number.floatValue(), 0);

        if (count > 0) {
            this.map.remove(number.floatValue());
            this.total -= count;
        }
        return this;
    }

    // Separator

    public float getPosition(Number number, float defaultValue) {
        Iterator<Map.Entry<Float, Integer>> iterator = this.map.entrySet().iterator();
        float position = 0.0f, find = number.floatValue();

        while (iterator.hasNext()) {
            Map.Entry<Float, Integer> next = iterator.next();

            if (next.getKey() == find) {
                if (next.getValue() > 1) {
                    return (position + (next.getValue() / 2.0f)) / this.total;
                } else {
                    return (position + 1.0f) / this.total;
                }
            } else if (next.getKey() > find) {
                return position / this.total;
            }
            position += next.getValue();
        }
        return defaultValue;
    }

    public float getPosition(Number number) {
        return this.getPosition(number, 1.0f);
    }

    public float getProbability(Number number, float defaultValue) {
        Integer count = this.map.get(number.floatValue());
        return count != null ? count / this.total : defaultValue;
    }

    public float getProbability(Number number) {
        return this.getProbability(number, 0.0f);
    }

    // Separator

    public boolean hasData() {
        return this.total > 0;
    }

    public int getTotal() {
        return (int) this.total;
    }
}
