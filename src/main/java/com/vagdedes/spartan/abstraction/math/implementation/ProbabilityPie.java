package com.vagdedes.spartan.abstraction.math.implementation;

import com.vagdedes.spartan.abstraction.math.AbstractPie;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class ProbabilityPie implements AbstractPie {

    private final Map<Float, Short> map;
    private float total;

    public ProbabilityPie() {
        this(-1);
    }

    public ProbabilityPie(int capacity) {
        this.map = capacity >= 0 ? new HashMap<>(capacity) : new HashMap<>();
        this.total = 0.0f;
    }

    public ProbabilityPie(int capacity, float loadFactor) {
        this.map = capacity >= 0 ? new HashMap<>(capacity, loadFactor) : new HashMap<>();
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

    public void create(Number number, int amount) {
        Short count = this.map.get(number.floatValue());

        if (count != null) {
            this.total -= count;
        }
        this.map.put(number.floatValue(), (short) amount);
        this.total += amount;
    }

    public void add(Number number) {
        this.increase(number, 1);
    }

    public void increase(Number number, int amount) {
        short current = this.map.getOrDefault(number.floatValue(), (short) 0);

        if (current < 32767) {
            this.map.put(
                    number.floatValue(),
                    (short) (current + amount)
            );
            this.total += amount;
        }
    }

    public void add(ProbabilityPie pie) {
        for (Map.Entry<Float, Short> entry : pie.map.entrySet()) {
            this.increase(entry.getKey(), entry.getValue());
        }
    }

    public void add(Collection<? extends Number> collection) {
        for (Number number : collection) {
            this.add(number);
        }
    }

    // Separator

    public void delete(Number number) {
        Short count = this.map.remove(number.floatValue());

        if (count != null) {
            this.total -= count;
        }
    }

    public boolean remove(Number number) {
        return this.decrease(number, 1);
    }

    public void remove(ProbabilityPie pie) {
        for (Map.Entry<Float, Short> entry : pie.map.entrySet()) {
            this.decrease(entry.getKey(), entry.getValue());
        }
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

    @Override
    public double getRatio(Number number) {
        return this.getRatio(number, 0.0);
    }

    @Override
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
        return this.total == 0;
    }

    @Override
    public int getSlices() {
        return this.map.size();
    }

    @Override
    public int getTotal() {
        return (int) this.total;
    }
}

