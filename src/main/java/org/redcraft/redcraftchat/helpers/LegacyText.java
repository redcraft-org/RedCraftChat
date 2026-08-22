package org.redcraft.redcraftchat.helpers;

import java.util.regex.Pattern;

/**
 * Legacy chat color codes helper.
 *
 * The whole message pipeline (translation, tokenization, Discord serialization)
 * works on legacy formatted strings, so this replaces the BungeeCord ChatColor
 * helpers with plain string constants and utilities.
 */
public class LegacyText {

    public static final char COLOR_CHAR = '§';

    public static final String BLACK = "§0";
    public static final String DARK_BLUE = "§1";
    public static final String DARK_GREEN = "§2";
    public static final String DARK_AQUA = "§3";
    public static final String DARK_RED = "§4";
    public static final String DARK_PURPLE = "§5";
    public static final String GOLD = "§6";
    public static final String GRAY = "§7";
    public static final String DARK_GRAY = "§8";
    public static final String BLUE = "§9";
    public static final String GREEN = "§a";
    public static final String AQUA = "§b";
    public static final String RED = "§c";
    public static final String LIGHT_PURPLE = "§d";
    public static final String YELLOW = "§e";
    public static final String WHITE = "§f";
    public static final String MAGIC = "§k";
    public static final String BOLD = "§l";
    public static final String STRIKETHROUGH = "§m";
    public static final String UNDERLINE = "§n";
    public static final String ITALIC = "§o";
    public static final String RESET = "§r";

    private static final String FORMATTING_CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx";

    private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)" + COLOR_CHAR + "[0-9A-FK-ORX]");

    private LegacyText() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
        char[] chars = textToTranslate.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == altColorChar && FORMATTING_CODES.indexOf(chars[i + 1]) > -1) {
                chars[i] = COLOR_CHAR;
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return new String(chars);
    }

    public static String stripColor(String input) {
        if (input == null) {
            return null;
        }
        return STRIP_COLOR_PATTERN.matcher(input).replaceAll("");
    }
}
