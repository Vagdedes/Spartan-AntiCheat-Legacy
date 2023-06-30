package me.vagdedes.spartan.utils.java;

import me.vagdedes.spartan.system.Cache;
import me.vagdedes.spartan.system.SpartanBukkit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collection;

public class MemoryUtils {

    public static int getNewestToOldestPosition(int size, int index) {
        return size - index - 1;
    }

    public static int sizeKB(Object object) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            oos.close();
            return baos.size() / Cache.gradeDivisor;
        } catch (IOException ignored) {
        }
        return 0;
    }

    public static <E> int fastHashCode(Collection<E> collection) {
        int size = collection.size();
        return size > 0 ? (size * SpartanBukkit.hashCodeMultiplier) + collection.iterator().next().hashCode() : 1;
    }
}
