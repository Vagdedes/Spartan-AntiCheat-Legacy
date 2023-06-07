package me.vagdedes.spartan.utils.java;

import me.vagdedes.spartan.system.Cache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Map;

public class MemoryUtils {

    public static int getNewestToOldestPosition(int size, int index) {
        return size - index - 1;
    }

    public static <K, V> int sizeKB(Map<K, V> map) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(map);
            oos.close();
            return baos.size() / Cache.gradeDivisor;
        } catch (IOException ignored) {
        }
        return 0;
    }

    public static <E> int sizeKB(Collection<E> collection) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(collection);
            oos.close();
            return baos.size() / Cache.gradeDivisor;
        } catch (IOException ignored) {
        }
        return 0;
    }
}
