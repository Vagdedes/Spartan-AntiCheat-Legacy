package com.vagdedes.spartan.abstraction.check.implementation.inventory;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.TPS;
import me.vagdedes.spartan.system.Enums;

import java.util.LinkedList;
import java.util.List;

public class ItemDrops extends CheckExecutor {

    private long time;
    private final Buffer.IndividualBuffer buffer;
    private final List<Long> times;

    public ItemDrops(Enums.HackType hackType, SpartanPlayer player) {
        super(hackType, player, 0, false);
        this.buffer = new Buffer.IndividualBuffer(player);
        this.times = new LinkedList<>();
    }

    @Override
    protected void runInternal(boolean cancelled) {
        long timePassed = System.currentTimeMillis() - this.time;
        this.time = System.currentTimeMillis();

        if (timePassed <= TPS.tickTime) {
            int count = buffer.count(1, 10);

            if (count == 1) {
                this.times.clear();
                this.times.add(timePassed);
            } else {
                this.times.add(timePassed);

                if (count >= 7) {
                    for (long timePassedStored : this.times) {
                        if (timePassedStored > 0L) {
                            cancel(
                                    "type: default",
                                    1.0,
                                    null,
                                    10
                            );
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    protected boolean canRun() {
        return !player.getInstance().isDead();
    }
}
