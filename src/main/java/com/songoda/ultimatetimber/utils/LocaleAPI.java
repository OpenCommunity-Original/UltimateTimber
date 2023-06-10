package com.songoda.ultimatetimber.utils;

import com.songoda.core.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLocaleChangeEvent;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.bukkit.Bukkit.getServer;

/**
 * The LocaleAPI class provides a simple and efficient way to manage player
 * locales and retrieve localized messages based on the player's locale.
 */
public class LocaleAPI implements Listener {
    private static final Map<Player, Locale> playerLocales = new HashMap<>();
    private static final Locale DEFAULT_LOCALE = Locale.US;
    private static final List<Locale> SUPPORTED_LOCALES = new ArrayList<>();
    private static String baseName;
    private static final Map<String, YamlConfiguration> configurationCache = new ConcurrentHashMap<>();

    /**
     * Sets the locale for a player.
     *
     * @param player The player to set the locale for
     * @param locale The locale to set
     */
    private static void setPlayerLocale(Player player, Locale locale) {
        playerLocales.put(player, locale);
    }

    /**
     * Loads a configuration file asynchronously.
     *
     * @param file The file to load
     * @return A CompletableFuture that will complete with the loaded configuration file or null if there was an error
     * @implNote The loaded configuration file is stored in cache for future use.
     */
    private static CompletableFuture<YamlConfiguration> loadConfigurationAsync(File file) {
        String key = file.getAbsolutePath();
        YamlConfiguration cachedConfig = configurationCache.get(key);
        if (cachedConfig != null) {
            return CompletableFuture.completedFuture(cachedConfig);
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                configurationCache.put(key, config);
                return config;
            } catch (Exception e) {
                Bukkit.getLogger().warning("Error loading configuration file: " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * Gets the player's locale. If the player doesn't have a locale set,
     * it will try to get the locale from the player's settings. If the
     * locale is supported, it will be set for the player. Otherwise, the
     * default locale will be set for the player.
     *
     * @param player the player
     * @return the player's locale
     */
    private static Locale getPlayerLocale(Player player) {
        if (playerLocales.containsKey(player)) {
            return playerLocales.get(player);
        }

        if (player == null) {
            return DEFAULT_LOCALE;
        }

        Locale locale = player.locale();

        if (isLocaleSupported(locale)) {
            setPlayerLocale(player, locale);
        } else {
            setPlayerLocale(player, DEFAULT_LOCALE);
            locale = DEFAULT_LOCALE;
        }

        return locale;
    }

    /**
     * Gets the message for the specified key and player's locale. If the
     * message isn't found for the player's locale, it will try to find it
     * for the default locale. If the message still isn't found, it will
     * return null.
     *
     * @param player       the player
     * @param key          the message key
     * @param placeholders the placeholders and their corresponding values to be replaced in the message
     * @return the message with placeholders replaced or null if not found
     */
    public static String getMessage(Player player, String key, String... placeholders) {
        Locale locale = getPlayerLocale(player);
        String lang = locale.toLanguageTag();
        File file = new File(baseName, lang + ".lang");

        CompletableFuture<YamlConfiguration> future = loadConfigurationAsync(file);
        String message = future.thenApply(config -> config.getString(key))
                .exceptionally(e -> {
                    Bukkit.getLogger().warning("Error getting message: " + e.getMessage());
                    return null;
                })
                .join();

        if (message == null) {
            CompletableFuture<YamlConfiguration> fallback = loadConfigurationAsync(new File(baseName, DEFAULT_LOCALE.toLanguageTag() + ".lang"));
            message = fallback.thenApply(config -> config.getString(key))
                    .exceptionally(e -> {
                        Bukkit.getLogger().warning("Error getting message: " + e.getMessage());
                        return null;
                    })
                    .join();
        }

        // replace placeholders
        for (int i = 0; i < placeholders.length; i += 2) {
            message = message.replace(placeholders[i], placeholders[i + 1]);
        }

        return message;
    }


    /**
     * Checks if the locale is supported.
     *
     * @param locale the locale
     * @return true if supported, false otherwise
     */
    private static boolean isLocaleSupported(Locale locale) {
        return SUPPORTED_LOCALES.contains(locale);
    }

    /**
     * Copies the Messages folder from the plugin resources to the plugin folder.
     * This is used to provide default message files.
     *
     * @param plugin The plugin
     */
    private static void copyMessages(Plugin plugin) {
        File sourceFolder = new File(plugin.getDataFolder(), "Messages");
        if (!sourceFolder.exists()) {
            sourceFolder.mkdirs();
        }

        try {
            // Open the plugin jar file as a ZipFile
            File pluginFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            ZipFile zipFile = new ZipFile(pluginFile);

            // Loop through the contents of the jar file to find the Messages folder
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (entryName.startsWith("Messages/") && !entry.isDirectory()) {
                    // Extract the file to the Messages folder in the plugin folder
                    File targetFile = new File(sourceFolder, entryName.substring("Messages/".length()));
                    if (!targetFile.getParentFile().exists()) {
                        targetFile.getParentFile().mkdirs();
                    }
                    InputStream inputStream = zipFile.getInputStream(entry);
                    Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    inputStream.close();
                }
            }

            zipFile.close();
        } catch (Exception e) {
            Bukkit.getLogger().warning("Error copying Messages folder from plugin resources: " + e.getMessage());
        }
    }


    /**
     * Loads the supported locales from the plugin's messages folder.
     * If no locales are found, the plugin will be disabled.
     *
     * @param plugin the plugin
     */
    public void loadSupportedLocales(Plugin plugin) {
        baseName = plugin.getDataFolder().getAbsolutePath() + File.separator + "Messages" + File.separator;
        File messagesFolder = new File(baseName);
        if (messagesFolder.listFiles() == null) {
            copyMessages(plugin);
        }
        File[] messageFiles = messagesFolder.listFiles();
        if (messageFiles != null) {
            for (File file : messageFiles) {
                String fileName = file.getName();
                if (fileName.endsWith(".lang")) {
                    String localeString = fileName.substring(0, fileName.indexOf(".lang"));
                    Locale locale = Locale.forLanguageTag(localeString.replace("_", "-"));
                    if (locale != null) {
                        SUPPORTED_LOCALES.add(locale);
                    }
                }
            }
        }
        if (SUPPORTED_LOCALES.isEmpty()) {
            Bukkit.getLogger().warning("Failed to load any language files.");
            getServer().getPluginManager().disablePlugin(plugin);
        }
    }

    /**
     * Handles the PlayerLocaleChangeEvent.
     * Updates the locale for the player if it is supported.
     * Otherwise sets the default locale.
     *
     * @param event The PlayerLocaleChangeEvent
     */
    @EventHandler
    public void onPlayerLocaleChange(PlayerLocaleChangeEvent event) {
        Player player = event.getPlayer();
        Locale locale = event.locale();
        if (isLocaleSupported(locale)) {
            setPlayerLocale(player, locale);
        } else {
            setPlayerLocale(player, DEFAULT_LOCALE);
        }
    }

    /**
     * Sends a localized message with a prefix to a command sender.
     *
     * @param sender       The command sender to send the message to.
     * @param messageKey   The key of the message to send.
     * @param placeholders The placeholders to replace in the message.
     */
    public static void sendPrefixedMessage(CommandSender sender, String messageKey, String... placeholders) {
        Player player = (Player) sender;
        String message = LocaleAPI.getMessage(player, messageKey);
        String prefix = LocaleAPI.getMessage(player, "general.nametag.prefix");

        if (sender instanceof Player) {
            for (int i = 0; i < placeholders.length; i += 2) {
                message = message.replace(placeholders[i], placeholders[i + 1]);
            }
            player.sendMessage(TextUtils.formatText((prefix == null ? "" : prefix + " ") + message));
        } else {
            for (int i = 0; i < placeholders.length; i += 2) {
                message = message.replace(placeholders[i], placeholders[i + 1]);
            }
            sender.sendMessage(TextUtils.formatText(message));
        }
    }

    public static String getFormattedMessage(Player player, String key, String... placeholders) {
        String message = getMessage(player, key, placeholders);
        return TextUtils.formatText(message);
    }

}
