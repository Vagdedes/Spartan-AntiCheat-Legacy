package ai.idealistic.spartan.abstraction.inventory.implementation;

import ai.idealistic.spartan.abstraction.check.Check;
import ai.idealistic.spartan.abstraction.check.CheckDetection;
import ai.idealistic.spartan.abstraction.check.CheckEnums;
import ai.idealistic.spartan.abstraction.inventory.InventoryMenu;
import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.spartan.api.Permission;
import ai.idealistic.spartan.functionality.connection.PluginAddons;
import ai.idealistic.spartan.functionality.moderation.clickable.ClickableMessage;
import ai.idealistic.spartan.functionality.server.PluginBase;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class StatisticsMenu extends InventoryMenu {

    public static final String title = "Plugin Statistics";

    public StatisticsMenu() {
        super(title, 54, new Permission[]{Permission.MANAGE, Permission.INFO});
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
        int startingLine = 1;

        for (CheckEnums.HackCategoryType categoryType : CheckEnums.HackCategoryType.values()) {
            int slot = (startingLine * 9) + 2;

            for (CheckEnums.HackType hackType : CheckEnums.HackType.values()) {
                if (hackType.category == categoryType) {
                    list.clear();
                    list.add("§6" + hackType.category.toString());
                    Collection<CheckDetection> detections = protocol.profile().getRunner(hackType).getDetections();

                    if (!detections.isEmpty()) {
                        list.add("");
                        list.add("§7Detection Levels§8:");

                        for (CheckDetection detection : detections) {
                            list.add("");
                            list.add("§7" + detection.name + "§8:");

                            for (Check.DataType dataType : Check.DataType.values()) {
                                list.add("§3In " + dataType + " notifies at§8: §b"
                                        + detection.getNotificationLevel(dataType));
                                list.add("§6In " + dataType + " prevents at§8: §e"
                                        + detection.getNotificationLevel(dataType));
                                list.add("§4In " + dataType + " punishes at level§8: §c"
                                        + detection.getNotificationLevel(dataType));
                            }
                        }
                    }
                    add("§3" + hackType.getCheck().getName(), list, new ItemStack(hackType.material), slot);
                    slot++;
                }
            }
            startingLine++;
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
        }
        return true;
    }

}
