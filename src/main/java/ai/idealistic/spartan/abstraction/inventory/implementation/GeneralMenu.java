package ai.idealistic.spartan.abstraction.inventory.implementation;

import ai.idealistic.spartan.abstraction.inventory.InventoryMenu;
import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.spartan.api.Permission;
import ai.idealistic.spartan.functionality.connection.PluginAddons;
import ai.idealistic.spartan.functionality.moderation.clickable.ClickableMessage;
import ai.idealistic.spartan.functionality.server.PluginBase;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class GeneralMenu extends InventoryMenu {

    public static final String title = "General Functionality";

    public GeneralMenu() {
        super(title, 45, Permission.MANAGE);
    }

    @Override
    public boolean internalOpen(PlayerProtocol protocol, boolean permissionMessage, Object object) {
        if (!PluginAddons.isSyn()) {
            protocol.bukkit().closeInventory();
            String missing = PluginAddons.synClick.replace(PluginAddons.synMissingPlaceholder, title);
            ClickableMessage.sendURL(
                    protocol.bukkit(),
                    missing,
                    missing + " (" + PluginAddons.synURL + ")",
                    PluginAddons.synURL
            );
            return false;
        }
        List<String> list = new ArrayList<>();
        add("§aLogs >> File", list, new ItemStack(Material.ARROW), 11);
        list.clear();
        add("§eLogs >> Console", list, new ItemStack(Material.ARROW), 12);
        list.clear();
        add("§eNotifications >> Individual Only", list, new ItemStack(Material.ARROW), 13);
        list.clear();
        add("§eNotifications >> Enable On Login", list, new ItemStack(Material.ARROW), 14);
        list.clear();
        add("§cNotifications >> Awareness", list, new ItemStack(Material.ARROW), 15);
        list.clear();

        add("§cImportant >> Op Bypass", list, new ItemStack(Material.ARROW), 20);
        list.clear();
        add("§cImportant >> Bedrock On ProtocolLib", list, new ItemStack(Material.ARROW), 21);
        list.clear();
        add("§eImportant >> Bedrock Client Permission", list, new ItemStack(Material.ARROW), 22);
        list.clear();
        add("§cImportant >> Enable Developer API", list, new ItemStack(Material.ARROW), 23);
        list.clear();
        add("§aImportant >> Enable Watermark", list, new ItemStack(Material.ARROW), 24);
        list.clear();

        add("§aImportant >> Enable NPC", list, new ItemStack(Material.ARROW), 30);
        list.clear();
        add("§cDetections >> Ground Teleport", list, new ItemStack(Material.ARROW), 31);
        list.clear();
        add("§cDetections >> Damage", list, new ItemStack(Material.ARROW), 32);

        add("§4Back", null, new ItemStack(Material.ARROW), 49);
        return true;
    }

    @Override
    public boolean internalHandle(PlayerProtocol protocol) {
        String item = itemStack.getItemMeta().getDisplayName();
        item = item.startsWith("§") ? item.substring(2) : item;

        if (item.equals("Back")) {
            PluginBase.synMenu.open(protocol, protocol.bukkit().getName());
        } else {
            item = item.split(" ")[0];
        }
        return true;
    }

}
