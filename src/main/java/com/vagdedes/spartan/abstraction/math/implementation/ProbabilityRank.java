package com.vagdedes.spartan.abstraction.math.implementation;

import com.vagdedes.spartan.abstraction.math.AbstractRank;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class ProbabilityRank implements AbstractRank {

    private final Map<Float, Short> map;
    private float total;

    public ProbabilityRank() {
        this.map = new TreeMap<>();
        this.total = 0.0f;
    }

    @Override
    public void clear() {
        this.map.clear();
        this.total = 0.0f;
    }

    // Separator

    public void addMultiple(Collection<? extends Number> numbers) {
        for (Number number : numbers) {
            this.map.put(
                    number.floatValue(),
                    (short) (this.map.getOrDefault(number.floatValue(), (short) 0) + 1)
            );
            this.total++;
        }
    }

    public void addMultiple(Map<? extends Number, ? extends Number> map) {
        for (Map.Entry<? extends Number, ? extends Number> entry : map.entrySet()) {
            this.map.put(
                    entry.getKey().floatValue(),
                    (short) (this.map.getOrDefault(entry.getKey().floatValue(), (short) 0) + entry.getValue().shortValue())
            );
            this.total += entry.getValue().shortValue();
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
            this.total++;
        }
    }

    // Separator

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

    public void delete(Number number) {
        short count = this.map.getOrDefault(number.floatValue(), (short) 0);

        if (count > 0) {
            this.map.remove(number.floatValue());
            this.total -= count;
        }
    }

    @Override
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

    @Override
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

    @Override
    public double getPosition(Number number) {
        return this.getPosition(number, 1.0);
    }

    // Separator

    @Override
    public double getRatio(Number number, double defaultValue) {
        Short count = this.map.get(number.floatValue());
        return count != null ? count / this.total : defaultValue;
    }

    @Override
    public double getRatio(Number number) {
        return this.getRatio(number, 0.0);
    }

    // Separator

    @Override
    public int getCount(Number number) {
        return this.getCount(number, 0);
    }

    @Override
    public int getCount(Number number, int defaultValue) {
        return this.map.getOrDefault(number.floatValue(), (short) defaultValue);
    }

    // Separator

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
}
