package cn.lunadeer.dominion.utils;

import org.apache.commons.lang3.tuple.Triple;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Misc {

    private static Boolean isPaper = null;

    /**
     * Checks if the server is running Paper.
     * <p>
     * This method attempts to load a specific class that is unique to Paper servers.
     * If the class is found, it indicates that the server is running Paper.
     * Otherwise, it returns false.
     *
     * @return true if the server is running Paper, false otherwise
     */
    public static boolean isPaper() {
        if (isPaper != null) return isPaper;
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
            isPaper = true;
            return true;
        } catch (ClassNotFoundException e) {
            isPaper = false;
            return false;
        }
    }

    /**
     * Formats a string by replacing placeholders with the provided arguments.
     * <p>
     * Each placeholder in the format `{index}` within the string is replaced
     * with the corresponding argument from the `args` array. If an argument is `null`,
     * it is replaced with a default string indicating the null value.
     *
     * @param str  the string containing placeholders to format
     * @param args the arguments to replace placeholders in the string
     * @return the formatted string
     */
    public static String formatString(String str, Object... args) {
        String formatStr = str;
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                args[i] = "[null for formatString (args[" + i + "])]";
            }
            formatStr = formatStr.replace("{" + i + "}", args[i].toString());
        }
        return formatStr;
    }

    /**
     * Formats a list of strings by replacing placeholders with the provided arguments.
     * <p>
     * Each placeholder in the format `{index}` within the strings in the list is replaced
     * with the corresponding argument from the `args` array. If an argument is `null`,
     * it is replaced with a default string indicating the null value.
     *
     * @param list the list of strings to format
     * @param args the arguments to replace placeholders in the strings
     * @return a new list of formatted strings
     */
    public static List<String> formatStringList(List<String> list, Object... args) {
        List<String> formattedList = new ArrayList<>(list);
        for (int i = 0; i < args.length; i++) {
            for (int j = 0; j < list.size(); j++) {
                formattedList.set(j, formattedList.get(j).replace("{" + i + "}", args[i].toString()));
            }
        }
        return formattedList;
    }

    /**
     * Splits a string into multiple lines based on the specified length.
     * <p>
     * If the string contains Chinese or Japanese characters, the length is halved
     * to account for the wider character width. The method ensures that the string
     * is split into lines of the specified length, adding ellipses ("...") if the
     * string exceeds twice the specified length.
     *
     * @param str    the string to be split into lines
     * @param length the maximum length of each line
     * @return a list of strings representing the split lines
     */
    public static List<String> foldLore2Line(String str, int length) {
        List<String> result = new ArrayList<>();
        // if str contains chinese or japanese characters, length should be adjusted 1/2
        if (str.codePoints().anyMatch(codePoint ->
                Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN ||
                        Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HIRAGANA ||
                        Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.KATAKANA)) {
            length /= 2;
        }
        if (str.length() > length) {
            result.add(str.substring(0, length));
            if (str.length() > 2 * length) {
                result.add(str.substring(length, 2 * length - 3) + "...");
            } else {
                result.add(str.substring(length));
            }
        } else {
            result.add(str);
            result.add("");
        }
        return result;
    }

    /**
     * Lists all classes in the specified package within the plugin's jar file.
     * <p>
     * This method retrieves the classes in the given package by inspecting the jar file
     * associated with the provided `JavaPlugin`. If the package is not found or the jar file
     * is invalid, an empty list is returned.
     *
     * @param plugin      the `JavaPlugin` instance to retrieve the jar file from
     * @param packageName the name of the package to list classes from
     * @return a list of fully qualified class names in the specified package
     */
    public static List<String> listClassOfPackage(JavaPlugin plugin, String packageName) {
        List<String> classesInPackage = new ArrayList<>();
        // list all classes in the packageName package
        String path = packageName.replace('.', '/');
        URL packageDir = plugin.getClass().getClassLoader().getResource(path);
        if (packageDir == null) {
            return classesInPackage;
        }
        String packageDirPath = packageDir.getPath();
        XLogger.debug("packageDirPath raw: {0}", packageDirPath);
        // if the package is in a jar file, unpack it and list the classes
        packageDirPath = packageDirPath.substring(0, packageDirPath.indexOf("jar!") + 4);
        packageDirPath = packageDirPath.replace("file:", "");
        packageDirPath = packageDirPath.replace("!", "");
        packageDirPath = java.net.URLDecoder.decode(packageDirPath, java.nio.charset.StandardCharsets.UTF_8);
        XLogger.debug("packageDirPath processed: {0}", packageDirPath);
        // unpack the jar file
        XLogger.debug("Unpacking class in jar: {0}", packageDirPath);
        File jarFile = new File(packageDirPath);
        if (!jarFile.exists() || !jarFile.isFile()) {
            XLogger.debug("Skipping {0} because it is not a jar file", packageDirPath);
            return classesInPackage;
        }
        // list the classes in the jar file
        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile)) {
            jar.stream().filter(entry -> entry.getName().endsWith(".class") && entry.getName().startsWith(path))
                    .forEach(entry -> classesInPackage.add(entry.getName().replace('/', '.').substring(0, entry.getName().length() - 6)));
        } catch (Exception e) {
            XLogger.debug("Failed to list classes in jar: {0}", e.getMessage());
            return classesInPackage;
        }
        return classesInPackage;
    }

    /**
     * Calculates pagination information based on the current page, page size, and total items.
     * <p>
     * Returns a Triple containing the start index (inclusive), end index (exclusive), and total number of pages.
     * Ensures that page and pageSize are at least 1, and that the page does not exceed the total number of pages.
     *
     * @param page      the current page number (1-based)
     * @param pageSize  the number of items per page
     * @param totalItem the total number of items
     * @return a Triple of (start index, end index, total pages)
     */
    public static Triple<Integer, Integer, Integer> pageUtil(int page, int pageSize, int totalItem) {
        if (totalItem <= 0) {
            return Triple.of(0, 0, 1);
        }
        if (page < 1) {
            page = 1;
        }
        if (pageSize < 1) {
            pageSize = 1;
        }
        int totalPage = (int) Math.ceil((double) totalItem / pageSize);
        if (page > totalPage) {
            page = totalPage;
        }
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, totalItem);
        return Triple.of(start, end, totalPage);
    }

}
