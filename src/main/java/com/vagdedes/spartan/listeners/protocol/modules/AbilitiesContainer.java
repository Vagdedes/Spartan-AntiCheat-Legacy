package com.vagdedes.spartan.listeners.protocol.modules;

public class AbilitiesContainer {
    private final boolean sprint;
    private final boolean fly;

    public AbilitiesContainer(boolean sprint, boolean fly) {
        this.sprint = sprint;
        this.fly = fly;
    }

    public boolean sprint() { return this.sprint; }
    public boolean fly() { return this.fly; }
}
