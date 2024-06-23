package com.vagdedes.spartan.abstraction.world;

import org.bukkit.Material;

public class SpartanBlockData {

    public final Material material;
    public final boolean  waterLogged;

    public SpartanBlockData(Material material, boolean waterLogged) {
        this.material = material;
        this.waterLogged = waterLogged;
    }

}
