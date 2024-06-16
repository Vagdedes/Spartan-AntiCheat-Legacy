package com.vagdedes.spartan.utils.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class OverflowMap<K, V> implements Map<K, V> {

    private final Map<K, V> map;
    private final int maxSize;

    public OverflowMap(Map<K, V> map, int maxSize) {
        this.map = map;
        this.maxSize = maxSize;
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return this.map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return this.map.get(key);
    }

    @Nullable
    @Override
    public V put(K key, V value) {
        V result = this.map.put(key, value);

        if (result == null
                && this.map.size() > this.maxSize) {
            Iterator<K> iterator = this.map.keySet().iterator();
            iterator.next();
            iterator.remove();
        }
        return result;
    }

    @Override
    public V remove(Object key) {
        return this.map.remove(key);
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        this.map.clear();
    }

    @NotNull
    @Override
    public Set<K> keySet() {
        return this.map.keySet();
    }

    @NotNull
    @Override
    public Collection<V> values() {
        return this.map.values();
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return this.map.entrySet();
    }
}
