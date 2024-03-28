package com.vagdedes.spartan.utils.math;

import java.util.*;
import java.util.stream.Stream;

public class ProbabilityPie {

    private final Map<Number, Integer> map;
    private double total;

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
        Integer count = this.map.get(number);

        if (count != null) {
            this.total -= count;
        }
        this.map.put(number, amount);
        this.total += amount;
        return this;
    }

    public ProbabilityPie add(Number number) {
        return this.increase(number, 1);
    }

    public ProbabilityPie increase(Number number, int amount) {
        this.map.put(number, this.map.getOrDefault(number, 0) + amount);
        this.total += amount;
        return this;
    }

    public ProbabilityPie add(ProbabilityPie pie) {
        for (Map.Entry<Number, Integer> entry : pie.map.entrySet()) {
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
        Integer count = this.map.remove(number);

        if (count != null) {
            this.total -= count;
        }
        return this;
    }

    public boolean remove(Number number) {
        return this.decrease(number, 1);
    }

    public ProbabilityPie remove(ProbabilityPie pie) {
        for (Map.Entry<Number, Integer> entry : pie.map.entrySet()) {
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

    public ProbabilityPie removeByEntry(Collection<Map.Entry<Number, Integer>> collection) {
        for (Map.Entry<Number, Integer> number : collection) {
            this.map.remove(number.getKey());
        }
        return this;
    }

    public Collection<Number> removeOld(int amount) {
        Collection<Number> collection = new ArrayList<>(amount);
        Iterator<Number> iterator = this.map.keySet().iterator();

        while (iterator.hasNext()) {
            collection.add(iterator.next());
            iterator.remove();

            if (--amount == 0) {
                break;
            }
        }
        return collection;
    }

    public Collection<Map.Entry<Number, Integer>> removeLeastSignificant(int amount) {
        Collection<Map.Entry<Number, Integer>> collection = new ArrayList<>(amount);
        Iterator<Map.Entry<Number, Integer>> iterator = this.map.entrySet().stream().sorted(
                Map.Entry.comparingByValue()
        ).iterator();

        while (iterator.hasNext()) {
            Map.Entry<Number, Integer> next = iterator.next();
            collection.add(next);
            this.map.remove(next.getKey());

            if (--amount == 0) {
                break;
            }
        }
        return collection;
    }

    public boolean decrease(Number number, int amount) {
        Integer count = this.map.get(number);

        if (count != null) {
            if (amount >= count) {
                this.map.remove(number);
                this.total -= count;
                return true;
            } else {
                this.map.put(number, count - amount);
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

    public double getProbability(Number number) {
        return this.getProbability(number, 0.0);
    }

    public double getProbability(Number number, double defaultValue) {
        Integer count = this.map.get(number);
        return count != null ? count / this.total : defaultValue;
    }

    public Stream<Map.Entry<Number, Integer>> getProbabilitiesRanked() {
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

