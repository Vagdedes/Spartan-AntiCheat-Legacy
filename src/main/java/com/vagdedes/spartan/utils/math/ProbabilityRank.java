package com.vagdedes.spartan.utils.math;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class ProbabilityRank {

    private final Map<Float, Short> map;
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
                    (short) (this.map.getOrDefault(number.floatValue(), (short) 0) + 1)
            );
            this.total++;
        }
        return this;
    }

    public ProbabilityRank addMultiple(Map<? extends Number, Integer> map) {
        for (Map.Entry<? extends Number, Integer> entry : map.entrySet()) {
            this.map.put(
                    entry.getKey().floatValue(),
                    (short) (this.map.getOrDefault(entry.getKey().floatValue(), (short) 0) + entry.getValue())
            );
            this.total += entry.getValue();
        }
        return this;
    }

    public ProbabilityRank add(Number number) {
        short current = this.map.getOrDefault(number.floatValue(), (short) 0);

        if (current < 32767) {
            this.map.put(
                    number.floatValue(),
                    (short) (current + 1)
            );
            this.total++;
        }
        return this;
    }

    public boolean remove(Number number) {
        short count = this.map.getOrDefault(number.floatValue(), (short) 0);

        if (count == 1) {
            this.map.remove(number.floatValue());
            this.total--;
            return true;
        } else {
            if (count > 0) {
                this.map.put(number.floatValue(), (short) (count - 1));
                this.total--;
            }
            return false;
        }
    }

    public ProbabilityRank delete(Number number) {
        short count = this.map.getOrDefault(number.floatValue(), (short) 0);

        if (count > 0) {
            this.map.remove(number.floatValue());
            this.total -= count;
        }
        return this;
    }

    public Float removeLeastSignificant() {
        float min = Float.MAX_VALUE;
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
        return key;
    }

    // Separator

    public double getPosition(Number number, double defaultValue) {
        Iterator<Map.Entry<Float, Short>> iterator = this.map.entrySet().iterator();
        double position = 0.0;
        float find = number.floatValue();

        while (iterator.hasNext()) {
            Map.Entry<Float, Short> next = iterator.next();

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
        Short count = this.map.get(number.floatValue());
        return count != null ? count / this.total : defaultValue;
    }

    public double getProbability(Number number) {
        return this.getProbability(number, 0.0);
    }

    // Separator

    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    public int getParts() {
        return this.map.size();
    }

    public int getTotal() {
        return (int) this.total;
    }

    public float getRawTotal() {
        return this.total;
    }
}
