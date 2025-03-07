package com.vagdedes.spartan.abstraction.protocol;

public class PlayerTrackers {

    public enum TrackerFamily {
        VELOCITY
    }

    public enum TrackerType {
        TELEPORT(TrackerFamily.VELOCITY),
        VEHICLE(TrackerFamily.VELOCITY); // MOTION

        public final TrackerFamily family;

        TrackerType(TrackerFamily family) {
            this.family = family;
        }
    }

    public PlayerTrackers() {
    }

    public boolean has(TrackerType trackerType, String key) {
        return false;
    }

}
