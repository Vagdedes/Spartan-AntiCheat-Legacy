package com.vagdedes.spartan.utils.math;

import java.util.*;
import java.util.stream.Stream;

public class ProbabilityPie {

    private final Map<Float, Integer> map;
    private float total;

    public ProbabilityPie() {
        this(-1);
    }

    public ProbabilityPie(int capacity) {
        this.map = capacity >= 0 ? new LinkedHashMap<>(capacity) : new LinkedHashMap<>();
        this.total = 0;
    }

    public ProbabilityPie clear() {
        this.map.clear();
        this.total = 0;
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
        Integer count = this.map.get(number.floatValue());

        if (count != null) {
            this.total -= count;
        }
        this.map.put(number.floatValue(), amount);
        this.total += amount;
        return this;
    }

    public ProbabilityPie add(Number number) {
        return this.increase(number, 1);
    }

    public ProbabilityPie increase(Number number, int amount) {
        this.map.put(number.floatValue(), this.map.getOrDefault(number.floatValue(), 0) + amount);
        this.total += amount;
        return this;
    }

    public ProbabilityPie add(ProbabilityPie pie) {
        for (Map.Entry<Float, Integer> entry : pie.map.entrySet()) {
            this.increase(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public ProbabilityPie add(Collection<Number> collection) {
        for (Number number : collection) {
            this.add(number);
        }
        return this;
    }

    // Separator

    public ProbabilityPie delete(Number number) {
        Integer count = this.map.remove(number.floatValue());

        if (count != null) {
            this.total -= count;
        }
        return this;
    }

    public boolean remove(Number number) {
        return this.decrease(number, 1);
    }

    public ProbabilityPie remove(ProbabilityPie pie) {
        for (Map.Entry<Float, Integer> entry : pie.map.entrySet()) {
            this.decrease(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public ProbabilityPie removeByCollection(Collection<Number> collection) {
        for (Number number : collection) {
            this.remove(number);
        }
        return this;
    }

    public ProbabilityPie removeByEntry(Collection<Map.Entry<Float, Integer>> collection) {
        for (Map.Entry<Float, Integer> number : collection) {
            this.map.remove(number.getKey());
        }
        return this;
    }

    public Collection<Float> removeOld(int amount) {
        Collection<Float> collection = new ArrayList<>(amount);
        Iterator<Float> iterator = this.map.keySet().iterator();

        while (iterator.hasNext()) {
            collection.add(iterator.next());
            iterator.remove();

            if (--amount == 0) {
                break;
            }
        }
        return collection;
    }

    public Number removeLeastSignificant() {
        float min = Float.MAX_VALUE;
        Number key = null;

        for (Map.Entry<Float, Integer> entry : this.map.entrySet()) {
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
        Integer count = this.map.get(number.floatValue());

        if (count != null) {
            if (amount >= count) {
                this.map.remove(number.floatValue());
                this.total -= count;
                return true;
            } else {
                this.map.put(number.floatValue(), count - amount);
                this.total -= amount;
                return false;
            }
        } else {
            return false;
        }
    }

    // Separator

    public float getProbabilityWithCount(int count) {
        return count / this.total;
    }

    public float getProbability(Number number) {
        return this.getProbability(number, 0.0f);
    }

    public float getProbability(Number number, float defaultValue) {
        Integer count = this.map.get(number);
        return count != null ? count / this.total : defaultValue;
    }

    public Stream<Map.Entry<Float, Integer>> getProbabilitiesRanked() {
        return this.map.entrySet().stream().sorted(
                Map.Entry.comparingByValue(Comparator.reverseOrder())
        );
    }

    // Separator

    public int getCount(Number number) {
        return this.getCount(number, 0);
    }

    public int getCount(Number number, int defaultValue) {
        return this.map.getOrDefault(number, defaultValue);
    }

    // Separator

    public int getSlices() {
        return this.map.size();
    }

    public int getTotal() {
        return (int) this.total;
    }

    public boolean hasData() {
        return this.total > 0;
    }
}

