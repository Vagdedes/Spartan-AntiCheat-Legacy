package com.vagdedes.spartan.utils.math;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class ProbabilityPie {

    private final Map<Float, Short> map;
    private float total;

    public ProbabilityPie() {
        this(-1);
    }

    public ProbabilityPie(int capacity) {
        this.map = capacity >= 0 ? new HashMap<>(capacity) : new HashMap<>();
        this.total = 0.0f;
    }

    public ProbabilityPie clear() {
        this.map.clear();
        this.total = 0.0f;
        return this;
    }

    // Separator

    public ProbabilityPie addMultiple(Collection<? extends Number> numbers) {
        for (Number number : numbers) {
            this.add(number);
        }
        return this;
    }

    // Separator

    public ProbabilityPie create(Number number, int amount) {
        Short count = this.map.get(number.floatValue());

        if (count != null) {
            this.total -= count;
        }
        this.map.put(number.floatValue(), (short) amount);
        this.total += amount;
        return this;
    }

    public ProbabilityPie add(Number number) {
        return this.increase(number, 1);
    }

    public ProbabilityPie increase(Number number, int amount) {
        short current = this.map.getOrDefault(number.floatValue(), (short) 0);

        if (current < 32767) {
            this.map.put(
                    number.floatValue(),
                    (short) (current + amount)
            );
            this.total += amount;
        }
        return this;
    }

    public ProbabilityPie add(ProbabilityPie pie) {
        for (Map.Entry<Float, Short> entry : pie.map.entrySet()) {
            this.increase(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public ProbabilityPie add(Collection<? extends Number> collection) {
        for (Number number : collection) {
            this.add(number);
        }
        return this;
    }

    // Separator

    public ProbabilityPie delete(Number number) {
        Short count = this.map.remove(number.floatValue());

        if (count != null) {
            this.total -= count;
        }
        return this;
    }

    public boolean remove(Number number) {
        return this.decrease(number, 1);
    }

    public ProbabilityPie remove(ProbabilityPie pie) {
        for (Map.Entry<Float, Short> entry : pie.map.entrySet()) {
            this.decrease(entry.getKey(), entry.getValue());
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

    public boolean decrease(Number number, int amount) {
        Short count = this.map.get(number.floatValue());

        if (count != null) {
            if (amount >= count) {
                this.map.remove(number.floatValue());
                this.total -= count;
                return true;
            } else {
                this.map.put(number.floatValue(), (short) (count - amount));
                this.total -= amount;
                return false;
            }
        } else {
            return false;
        }
    }

    // Separator

    public double getProbabilityWithCount(int count) {
        return count / this.total;
    }

    public double getRatio(Number number) {
        return this.getRatio(number, 0.0);
    }

    public double getRatio(Number number, double defaultValue) {
        Short count = this.map.get(number.floatValue());
        return count != null ? count / this.total : defaultValue;
    }

    public Stream<Map.Entry<Float, Short>> getProbabilitiesRanked() {
        return this.map.entrySet().stream().sorted(
                Map.Entry.comparingByValue(Comparator.reverseOrder())
        );
    }

    // Separator

    public int getCount(Number number) {
        return this.getCount(number, 0);
    }

    public int getCount(Number number, int defaultValue) {
        return this.map.getOrDefault(number.floatValue(), (short) defaultValue);
    }

    // Separator

    public boolean isEmpty() {
        return this.total == 0;
    }

    public int getSlices() {
        return this.map.size();
    }

    public int getTotal() {
        return (int) this.total;
    }

    public float getRawTotal() {
        return this.total;
    }
}

