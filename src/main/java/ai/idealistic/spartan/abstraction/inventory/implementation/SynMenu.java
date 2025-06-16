package ai.idealistic.spartan.abstraction.inventory.implementation;

import ai.idealistic.spartan.abstraction.inventory.InventoryMenu;
import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.spartan.api.Permission;
import ai.idealistic.spartan.functionality.connection.PluginAddons;
import ai.idealistic.spartan.functionality.moderation.clickable.ClickableMessage;
import ai.idealistic.spartan.functionality.server.PluginBase;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class SynMenu extends InventoryMenu {

    public SynMenu() {
        super(PluginAddons.synName, 36, Permission.MANAGE);
    }

    @Override
    public boolean internalOpen(PlayerProtocol protocol, boolean permissionMessage, Object object) {
        add("§2" + EditionMenu.title, null, new ItemStack(Material.ARROW), 11);
        add("§2" + DetailsMenu.title, null, new ItemStack(Material.ARROW), 12);

        add("§2" + PreventionsMenu.title, null, new ItemStack(Material.ARROW), 14);
        add("§2" + PunishmentsMenu.title, null, new ItemStack(Material.ARROW), 15);

        add("§2", null, new ItemStack(Material.ARROW), 21);
        add("§2", null, new ItemStack(Material.ARROW), 22);
        add("§2", null, new ItemStack(Material.ARROW), 23);
        return true;
    }

    @Override
    public boolean internalHandle(PlayerProtocol protocol) {
        String item = itemStack.getItemMeta().getDisplayName();
        item = item.startsWith("§") ? item.substring(2) : item;

        if (item.equals(EditionMenu.title)) {
            PluginBase.editionMenu.open(protocol, protocol.bukkit().getName());
        } else if (item.equals(DetailsMenu.title)) {
            PluginBase.detailsMenu.open(protocol, protocol.bukkit().getName());
        } else if (item.equals(PreventionsMenu.title)) {
            PluginBase.preventionsMenu.open(protocol, protocol.bukkit().getName());
        } else if (item.equals(PunishmentsMenu.title)) {
            PluginBase.punishmentsMenu.open(protocol, protocol.bukkit().getName());
        }
        return true;
    }

}
