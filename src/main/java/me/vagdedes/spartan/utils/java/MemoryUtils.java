package me.vagdedes.spartan.utils.java;

import me.vagdedes.spartan.system.Cache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

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
}
