package me.vagdedes.spartan.abstraction;

import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public abstract class InventoryMenu {

    private String title;
    private boolean equals;
    private ItemStack itemStack;
    private ClickType clickType;
    private int slot;

    public InventoryMenu(String title, boolean equals) {
        this.title = title;
        this.equals = equals;
        this.itemStack = null;
        this.clickType = null;
        this.slot = 0;
    }

    public abstract void open(SpartanPlayer player);

    public void handle(SpartanPlayer player, String title, ItemStack itemStack, ClickType clickType, int slot) {
        if (equals ? title.equals(this.title) : title.startsWith(this.title)) {
            this.itemStack = itemStack;
            this.clickType = clickType;
            this.slot = slot;
            handle(player);
        }
    }

    protected abstract void handle(SpartanPlayer player);
}
