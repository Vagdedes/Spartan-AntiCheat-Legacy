package me.vagdedes.spartan.handlers.connection;

import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.system.Threads;
import me.vagdedes.spartan.utils.java.RequestUtils;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;

public class PluginServerCount {

    private static int count = 0;
    private static int times = 0;

    public static void ignore() {
        times += 1;
    }

    public static int get() {
        if (times == 0) {
            times = 20;

            Threads.execute(SpartanBukkit.connectionThread, () -> {
                try {
                    String number = RequestUtils.get("https://vagdedes.com/spartan/statistics/serverCount/")[0];

                    if (AlgebraUtils.validInteger(number)) {
                        count = Integer.parseInt(number);
                    } else {
                        count = 0;
                    }
                } catch (Exception ex) {
                    count = 0;
                }
            });
        } else {
            times -= 1;
        }
        return count;
    }
}
