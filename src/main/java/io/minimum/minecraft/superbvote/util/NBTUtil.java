package io.minimum.minecraft.superbvote.util;

import java.util.ArrayList;
import java.util.List;

public class NBTUtil {

    /**
     * Replaces useless " on end/start.
     *
     * @param string String to parse.
     * @return Outcome. :"P
     */
    public static String strip(String string) {
        StringBuffer buf;
        buf = new StringBuffer(string);
        if (buf.charAt(0) == '"') buf.setCharAt(0, ' ');
        if (buf.charAt(string.length() - 1) == '"') buf.setCharAt(string.length() - 1, ' ');
        return buf.toString().trim();
    }

    /**
     * Replaces useless " on end/start.
     * VarChar edition
     *
     * @param stringArray to parse
     * @return String Array parsed..
     */
    public static List<String> strip(String... stringArray) {
        List<String> editedArray = new ArrayList<>();
        for (String string : stringArray) {
            StringBuffer buf;
            buf = new StringBuffer(string);
            if (buf.charAt(0) == '"') buf.setCharAt(0, ' ');
            if (buf.charAt(string.length() - 1) == '"') buf.setCharAt(string.length() - 1, ' ');
            editedArray.add(buf.toString().trim());
        }
        return editedArray;
    }
}
