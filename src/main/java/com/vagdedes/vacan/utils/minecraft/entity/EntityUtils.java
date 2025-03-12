package com.vagdedes.vacan.utils.minecraft.entity;

import com.vagdedes.vacan.utils.java.ReflectionUtils;

public class EntityUtils {

    public static final boolean abstractHorseClass = ReflectionUtils.classExists(
            "org.bukkit.entity.AbstractHorse"
    );

}
