package ai.idealistic.spartan.functionality.connection;

import ai.idealistic.spartan.Register;
import ai.idealistic.spartan.abstraction.check.Check;
import ai.idealistic.spartan.functionality.moderation.AwarenessNotifications;
import ai.idealistic.spartan.functionality.moderation.CrossServerNotifications;
import ai.idealistic.spartan.functionality.server.Config;
import ai.idealistic.spartan.functionality.server.PluginBase;
import ai.idealistic.spartan.utils.java.RequestUtils;
import ai.idealistic.spartan.utils.java.StringUtils;
import ai.idealistic.spartan.utils.math.AlgebraUtils;
import org.bukkit.ChatColor;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class CloudConnections {

    static int getUserIdentification() {
        try {
            String[] reply = RequestUtils.get(StringUtils.decodeBase64(JarVerification.website)
                            + "?action=get"
                            + "&data=userIdentification"
                            + "&version=" + Register.plugin.getDescription().getVersion(),
                    RequestUtils.defaultTimeOut);

            if (reply.length > 0) {
                String line = reply[0];

                if (AlgebraUtils.validInteger(line)) {
                    int id = Integer.parseInt(line);
                    IDs.set(id, id);
                    return id;
                }
            }
        } catch (Exception e) {
            CloudBase.throwError(e, "userIdentification:GET");
            return 0;
        }
        return -1;
    }

    static String[][] getStaffAnnouncements() {
        try {
            String[] results = RequestUtils.get(StringUtils.decodeBase64(JarVerification.website) + "?" + CloudBase.identification()
                    + "&action=get&data=staffAnnouncements&version=" + Register.plugin.getDescription().getVersion());

            if (results.length > 0) {
                String[] announcements = results[0].split(CloudBase.separator);
                String[][] array = new String[results.length][0];

                for (int i = 0; i < announcements.length; i++) {
                    array[i] = StringUtils.decodeBase64(announcements[i]).split(CloudBase.separator);
                }
                return array;
            }
        } catch (Exception e) {
            CloudBase.throwError(e, "staffAnnouncements:GET");
        }
        return new String[][]{};
    }

    static String[] getOwnedEditions() {
        try {
            String[] results = RequestUtils.get(
                    StringUtils.decodeBase64(JarVerification.website)
                            + "?" + CloudBase.identification()
                            + "&action=get"
                            + "&data=ownedEditions"
                            + "&email_address=" + URLEncoder.encode(Config.settings.getString("Purchases.email_address"), "UTF-8")
                            + "&patreon_full_name=" + URLEncoder.encode(Config.settings.getString("Purchases.patreon_full_name"), "UTF-8")
                            + "&version=" + Register.plugin.getDescription().getVersion()
            );

            if (results.length > 0) {
                return results[0].split(CloudBase.separator, Check.DataType.values().length);
            }
        } catch (Exception e) {
            CloudBase.throwError(e, "ownedEditions:GET");
        }
        if (IDs.enabled) {
            return new String[]{
                    Check.DataType.JAVA.toString(),
                    Check.DataType.BEDROCK.toString()
            };
        } else {
            return null;
        }
    }

    public static void executeDiscordWebhook(String webhook, UUID uuid, String name, int x, int y, int z, String type, String information) {
        String urlString = Config.settings.getString("Discord." + webhook + "_webhook_url");

        if (urlString == null
                || (!urlString.startsWith("https://") && !urlString.startsWith("http://"))) {
            return;
        }
        PluginBase.connectionThread.executeIfUnknownThreadElseHere(() -> {
            try {
                String serverName = CrossServerNotifications.getServerName();
                String titleSuffix = (serverName != null && !serverName.isEmpty() && !serverName.equals("NULL"))
                        ? " (" + serverName + ")"
                        : "";
                String colorHex = Config.settings.getString("Discord.webhook_hex_color");
                int colorDecimal = 0;

                try {
                    if (colorHex != null && !colorHex.isEmpty()) {
                        colorDecimal = Integer.parseInt(colorHex.replace("#", ""), 16);
                    }
                } catch (NumberFormatException ignored) {
                }
                String avatarUrl = "https://mc-heads.net/avatar/" + uuid.toString() + "/64";
                String safeName = escapeJson(truncate(name, 32));
                String safeUuid = escapeJson(truncate(uuid.toString(), 1024));
                String safeType = escapeJson(truncate(type, 256));
                String safeInfo = escapeJson(truncate(ChatColor.stripColor(information).replace("\n", " | "), 1024));
                String safeTitle = escapeJson(truncate(titleSuffix, 224));
                String jsonPayload = "{"
                        + "\"username\": \"Spartan AntiCheat\","
                        + "\"avatar_url\": \"https://vagdedes.com/.images/spartan/logo.png\","
                        + "\"embeds\": [{"
                        + "\"color\": " + colorDecimal + ","
                        + "\"author\": {"
                        + "\"name\": \"" + safeName + safeTitle + "\","
                        + "\"icon_url\": \"" + avatarUrl + "\""
                        + "},"
                        + "\"fields\": ["
                        + "{\"name\": \"" + safeName + "\", \"value\": \"``" + safeUuid + "``\", \"inline\": true},"
                        + "{\"name\": \"X, Y, Z\", \"value\": \"``" + x + "``**,** ``" + y + "``**,** ``" + z + "``\", \"inline\": true},"
                        + "{\"name\": \"" + safeType + "\", \"value\": \"``" + safeInfo + "``\", \"inline\": false}"
                        + "]"
                        + "}]"
                        + "}";
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "Java-Discord-Webhook");

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                int responseCode = connection.getResponseCode();

                if (responseCode < 200
                        || responseCode > 299) {
                    AwarenessNotifications.optionallySend("Webhook failed: " + responseCode);
                }

            } catch (Exception e) {
                CloudBase.throwError(e, "discordWebhooks:LOCAL");
            }
        });
    }

    private static String escapeJson(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();

        for (char c : raw.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < ' ') {
                        continue;
                    }
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() > maxLength ? text.substring(0, maxLength - 3) + "..." : text;
    }

}
