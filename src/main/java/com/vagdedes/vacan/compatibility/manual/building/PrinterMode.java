package com.vagdedes.vacan.compatibility.manual.building;

import com.vagdedes.vacan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.vacan.compatibility.Compatibility;
import me.gwndaan.printer.PrinterModeAPI;

public class PrinterMode {

    public static boolean isUsing(PlayerProtocol p) {
        return Compatibility.CompatibilityType.PRINTER_MODE.isFunctional()
                && PrinterModeAPI.isInPrinterMode(p.bukkit());
    }
}
