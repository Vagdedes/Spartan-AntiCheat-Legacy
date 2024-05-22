package com.vagdedes.spartan.abstraction.check.implementation.movement.speed;

import org.bukkit.entity.Entity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DataProcessor {

    public static final Map<UUID, KeepSprint> keepSprint = new ConcurrentHashMap<>();
    public static final Map<UUID, Entity> target = new ConcurrentHashMap<>();
}
