package com.vagdedes.vacan.compatibility.necessary;

import com.vagdedes.vacan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.vacan.functionality.server.Config;
import com.vagdedes.vacan.functionality.server.Permissions;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Player;

public class BedrockCompatibility {

    public static boolean isPlayer(Player p) {
        return ProtocolSupport.isBedrockPlayer(p)
                
                || !ProtocolLib.isTemporary(p)
                && Floodgate.isBedrockPlayer(p.getUniqueId(), p.getName())

                || Config.settings.getBoolean("Important.bedrock_client_permission")
                && Permissions.onlyHas(p, Enums.Permission.BEDROCK);
    }

}
