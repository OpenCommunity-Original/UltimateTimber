package com.songoda.core.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.songoda.core.compatibility.ClassMapping;
import com.songoda.core.compatibility.ServerVersion;
import com.songoda.core.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatMessage {
    private static final Gson gson = new GsonBuilder().create();
    private static boolean enabled = ServerVersion.isServerVersionAtLeast(ServerVersion.V1_8);
    private static Class<?> mc_ChatMessageType;

    static {
        init();
    }

    private final List<JsonObject> textList = new ArrayList<>();

    static void init() {
        if (enabled) {
            try {
                mc_ChatMessageType = ClassMapping.CHAT_MESSAGE_TYPE.getClazz();
            } catch (Throwable ex) {
                Bukkit.getLogger().log(Level.WARNING, "Problem preparing raw chat packets (disabling further packets)", ex);
                enabled = false;
            }
        }
    }

    public ChatMessage fromText(String text) {
        return fromText(text, false);
    }

    public ChatMessage fromText(String text, boolean noHex) {
        Pattern pattern = Pattern.compile(
                "(.*?)(?!&([omnlk]))(?=(&([123456789abcdefr#])|$)|#([a-f]|[A-F]|[0-9]){6})",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            ColorContainer color = null;
            String match1 = matcher.group(1);

            if (matcher.groupCount() == 0 || match1.length() == 0) {
                continue;
            }

            char colorChar = '-';

            if (matcher.start() != 0) {
                colorChar = text.substring(matcher.start() - 1, matcher.start()).charAt(0);
            }

            if (colorChar != '-') {
                if (colorChar == '#') {
                    color = new ColorContainer(match1.substring(0, 6), noHex);
                    match1 = match1.substring(5);
                } else if (colorChar == '&') {
                    color = new ColorContainer(ColorCode.getByChar(Character.toLowerCase(match1.charAt(0))));
                }
            }

            Pattern subPattern = Pattern.compile("(.*?)(?=&([omnlk])|$)");
            Matcher subMatcher = subPattern.matcher(match1);

            List<ColorCode> stackedCodes = new ArrayList<>();
            while (subMatcher.find()) {
                String match2 = subMatcher.group(1);
                if (match2.length() == 0) continue;

                ColorCode code = ColorCode.getByChar(Character.toLowerCase(match2.charAt(0)));

                if (code != null && code != ColorCode.RESET) {
                    stackedCodes.add(code);
                }

                if (color != null) {
                    match2 = match2.substring(1);
                }

                if (match2.length() != 0) {
                    addMessage(match2, color, stackedCodes);
                }
            }
        }

        return this;
    }

    public String toText() {
        return toText(false);
    }

    public String toText(boolean noHex) {
        StringBuilder text = new StringBuilder();

        for (JsonObject object : textList) {
            if (object.has("color")) {
                String color = object.get("color").getAsString();
                text.append("&");

                if (color.length() == 7) {
                    text.append(new ColorContainer(color, noHex).getColor().getCode());
                } else {
                    text.append(ColorCode.valueOf(color.toUpperCase()).getCode());
                }
            }

            for (ColorCode code : ColorCode.values()) {
                if (code.isColor()) continue;

                String c = code.name().toLowerCase();
                if (object.has(c) && object.get(c).getAsBoolean()) {
                    text.append("&").append(code.getCode());
                }
            }

            text.append(object.get("text").getAsString());
        }

        return text.toString();
    }

    public ChatMessage addMessage(String s) {
        JsonObject txt = new JsonObject();
        txt.addProperty("text", s);

        textList.add(txt);

        return this;
    }

    public ChatMessage addMessage(String text, ColorContainer color, List<ColorCode> colorCodes) {
        JsonObject txt = new JsonObject();
        txt.addProperty("text", text);

        if (color != null) {
            txt.addProperty("color", color.getHexCode() != null ? "#" + color.getHexCode() : color.getColorCode().name().toLowerCase());
        }

        for (ColorCode code : ColorCode.values()) {
            if (!code.isColor()) {
                txt.addProperty(code.name().toLowerCase(), colorCodes.contains(code));
            }
        }

        textList.add(txt);
        return this;
    }

    public ChatMessage addPromptCommand(String text, String hoverText, String cmd) {
        JsonObject txt = new JsonObject();
        txt.addProperty("text", text);

        JsonObject hover = new JsonObject();
        hover.addProperty("action", "show_text");
        hover.addProperty("value", hoverText);
        txt.add("hoverEvent", hover);

        JsonObject click = new JsonObject();
        click.addProperty("action", "suggest_command");
        click.addProperty("value", cmd);
        txt.add("clickEvent", click);

        textList.add(txt);
        return this;
    }

    @Override
    public String toString() {
        return gson.toJson(textList);
    }

    public void sendTo(CommandSender sender) {
        sendTo(null, sender);
    }

    public void sendTo(ChatMessage prefix, CommandSender sender) {
        /*if (!(sender instanceof Player) || !enabled) {
            try {
                List<JsonObject> textList = prefix == null ? new ArrayList<>() : new ArrayList<>(prefix.textList);
                textList.addAll(this.textList);

                Object packet;
                if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_19)) {
                    packet = mc_PacketPlayOutChat_new.newInstance(mc_IChatBaseComponent_ChatSerializer_a.invoke(null, gson.toJson(textList)), mc_PacketPlayOutChat_new_1_19_0 ? 1 : true);
                } else if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_16)) {
                    packet = mc_PacketPlayOutChat_new.newInstance(
                            mc_IChatBaseComponent_ChatSerializer_a.invoke(null, gson.toJson(textList)),
                            mc_chatMessageType_Chat.get(null),
                            ((Player) sender).getUniqueId());
                } else {
                    packet = mc_PacketPlayOutChat_new.newInstance(mc_IChatBaseComponent_ChatSerializer_a.invoke(null, gson.toJson(textList)));
                }
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException |
                     InvocationTargetException ex) {
                Bukkit.getLogger().log(Level.WARNING, "Problem preparing raw chat packets (disabling further packets)", ex);
                enabled = false;
            }

            return;
        }*/

        sender.sendMessage(TextUtils.formatText((prefix == null ? "" : prefix.toText(true) + " ") + toText(true)));
    }

}
