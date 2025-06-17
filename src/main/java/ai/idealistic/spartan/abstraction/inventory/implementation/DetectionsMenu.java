package ai.idealistic.spartan.abstraction.inventory.implementation;

import ai.idealistic.spartan.abstraction.inventory.InventoryMenu;
import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.spartan.api.Permission;
import ai.idealistic.spartan.functionality.connection.PluginAddons;
import ai.idealistic.spartan.functionality.moderation.clickable.ClickableMessage;
import ai.idealistic.spartan.functionality.server.PluginBase;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class DetectionsMenu extends InventoryMenu {

    public static final String title = "Check Detections";

    public DetectionsMenu() {
        super(title, 54, Permission.MANAGE);
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
