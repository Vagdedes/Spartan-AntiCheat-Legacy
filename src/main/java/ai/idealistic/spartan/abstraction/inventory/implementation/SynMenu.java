package ai.idealistic.spartan.abstraction.inventory.implementation;

import ai.idealistic.spartan.abstraction.inventory.InventoryMenu;
import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.spartan.api.Permission;
import ai.idealistic.spartan.functionality.connection.PluginAddons;
import ai.idealistic.spartan.functionality.server.PluginBase;
import ai.idealistic.spartan.utils.minecraft.inventory.MaterialUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SynMenu extends InventoryMenu {

    public static final Permission[] permissions = new Permission[]{Permission.MANAGE, Permission.INFO};

    public SynMenu() {
        super(PluginAddons.synName, 45, permissions);
    }

    @Override
    public boolean internalOpen(PlayerProtocol protocol, boolean permissionMessage, Object object) {
        List<String> list = new ArrayList<>();
        list.add("");
        add("§2" + EditionMenu.title, list, new ItemStack(Material.LEVER), 11);
        list.clear();
        list.add("");
        add("§2" + DetailsMenu.title, list, new ItemStack(Material.PAPER), 12);

        list.clear();
        list.add("");
        add("§2" + PreventionsMenu.title, list, new ItemStack(MaterialUtils.get("lead")), 14);
        list.clear();
        list.add("");
        add("§2" + PunishmentsMenu.title, list, new ItemStack(Material.IRON_AXE), 15);

        list.clear();
        list.add("");
        add("§2", list, new ItemStack(Material.ARROW), 30);
        list.clear();
        list.add("");
        add("§2", list, new ItemStack(Material.ARROW), 31);
        list.clear();
        list.add("");
        add("§2", list, new ItemStack(Material.ARROW), 32);
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
