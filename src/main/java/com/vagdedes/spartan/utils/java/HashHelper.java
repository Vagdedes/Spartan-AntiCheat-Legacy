package com.vagdedes.spartan.utils.java;

import com.vagdedes.spartan.system.SpartanBukkit;

import java.util.Collection;
import java.util.Iterator;

public class HashHelper {

    public static int fastCollection(Collection<? extends Number> collection) {
        return collection.isEmpty() ? 0 :
                (collection.iterator().next().hashCode() * SpartanBukkit.hashCodeMultiplier)
                        + collection.size();
    }

    public static int collection(Collection<? extends Number> collection) {
        int result = 1;

        for (Number num : collection) {
            result = (result * SpartanBukkit.hashCodeMultiplier) + num.hashCode();
        }
        return result;
    }

    public static int collection(Collection<? extends Number> collection,
                                 int start, int end) {
        int result = 1, pos = 0;
        Iterator<? extends Number> iterator = collection.iterator();

        if (start > 0) {
            for (int i = 0; i < start; i++) {
                iterator.next();
            }
        }
        while (iterator.hasNext()) {
            if (pos >= end) {
                break;
            } else {
                result = (result * SpartanBukkit.hashCodeMultiplier) + iterator.next().hashCode();
                pos++;
            }
        }
        return result;
    }
}
