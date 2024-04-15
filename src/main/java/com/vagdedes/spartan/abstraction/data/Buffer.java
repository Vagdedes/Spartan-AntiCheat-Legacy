package com.vagdedes.spartan.abstraction.data;

import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.TPS;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class Buffer {

    private final Map<String, BufferChild> storage;
    private final SpartanPlayer player;

    private static class BufferChild {

        private final Buffer parent;
        private long count, start;

        private BufferChild(Buffer buffer) {
            this.parent = buffer;
            this.reset();
        }

        private double ticksPassed() {
            return TPS.getTick(this.parent.player) - this.start;
        }

        private int increase(int amount) {
            long tick = TPS.getTick(this.parent.player);

            if (this.count <= tick) {
                this.count = tick + amount;
                this.start = tick;
                return 1;
            } else {
                this.count += amount;
                return this.simpleCount(tick);
            }
        }

        private int decrease(int amount) {
            if (this.count > amount) {
                this.count -= amount;
                return this.simpleCount(TPS.getTick(this.parent.player));
            } else {
                this.count = 0L;
                return 0;
            }
        }

        private int count() {
            long tick = TPS.getTick(this.parent.player);
            return this.count <= tick ? 0 : this.simpleCount(tick);
        }

        private int simpleCount(long tick) {
            return (int) (this.count - tick);
        }

        private void reset() {
            this.count = 0L;
            this.start = 0L;
        }
    }

    public Buffer(SpartanPlayer player) {
        this.player = player;
        this.storage = Collections.synchronizedMap(new LinkedHashMap<>());
    }

    // Implementation

    public void clear() {
        synchronized (storage) {
            storage.clear();
        }
    }

    public int get(String name) {
        BufferChild obj;

        synchronized (storage) {
            obj = storage.get(name);
        }
        return obj != null ? obj.count() : 0;
    }

    public void set(String name, int amount) {
        BufferChild obj;

        synchronized (storage) {
            obj = storage.get(name);

            if (obj == null) {
                obj = new BufferChild(this);
                storage.put(name, obj);
            }
        }
        obj.reset();
        obj.increase(amount);
    }

    public int increase(String name, int amount) {
        BufferChild obj;

        synchronized (storage) {
            obj = storage.computeIfAbsent(
                    name,
                    k -> new BufferChild(this)
            );
        }
        return obj.increase(amount);
    }

    public int decrease(String name, int amount) {
        BufferChild obj;

        synchronized (storage) {
            obj = storage.get(name);
        }
        return obj != null ? obj.decrease(amount) : 0;
    }

    public double start(String name, int minimumTicks, int maxTicks) {
        BufferChild obj;

        synchronized (storage) {
            obj = storage.computeIfAbsent(name, k -> new BufferChild(this));
        }
        double ticksPassed = obj.ticksPassed();
        int count;

        if (ticksPassed > maxTicks) {
            obj.reset();
            count = obj.increase(1);
        } else {
            count = obj.increase(1);
        }
        return count >= minimumTicks ? count / ticksPassed : 0.0;
    }

    public boolean canDo(String name) {
        return get(name) >= 0;
    }

    public void remove(String name) {
        synchronized (storage) {
            storage.remove(name);
        }
    }

    public void remove(String[] names) {
        synchronized (storage) {
            for (String name : names) {
                storage.remove(name);
            }
        }
    }

    public void clear(String s) {
        if (!storage.isEmpty()) {
            synchronized (storage) {
                Iterator<String> iterator = storage.keySet().iterator();

                while (iterator.hasNext()) {
                    if (iterator.next().contains(s)) {
                        iterator.remove();
                    }
                }
            }
        }
    }

}
