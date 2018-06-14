package com.squarepolka.readyci.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class Util {

    public static String getMappedValueAtPath(Map map, String path) {
        String[] pathKeys = path.split("\\.");
        Object currentObject = map;
        for (String key : pathKeys) {

            // Traverse the map type objects
            if (null != currentObject && currentObject instanceof Map) {
                currentObject = ((Map) currentObject).get(key);
            } else if (null != currentObject && currentObject instanceof List) {
                List listItems = (List) currentObject;
                currentObject = parseListObject(listItems, key);
            }

        }

        // If we've landed on a String, return it
        if (null != currentObject && currentObject instanceof String) {
            return (String) currentObject;
        }

        // If all else, return an empty string
        return "";
    }


    public static Object parseListObject(List listItems, String key) {
        // Find a map in this list which contains the key we're looking for
        for (Object listObject : listItems) {
            if (listObject instanceof Map) {
                Map map = (Map) listObject;
                if (map.containsKey(key)) {
                    return map.get(key);
                }
            }
        }
        return null;
    }

    public static boolean valueExists(String string) {
        return string.length() > 0;
    }

    /**
     * Skip half of the available data in an input stream.
     * This method is useful when a full input stream might block a process,
     * while keeping some data might be desired.
     *
     * Unfortunately, an IOException is sometimes thrown when using the processInputStream.skip(long n) method.
     * We're using processInputStream.read() instead.
     * We might be suffering from this: https://bugs.java.com/view_bug.do?bug_id=6222822
     *
     * @param inputStream
     */
    public static void skipHalfOfStream(InputStream inputStream) throws IOException {
        int availableBytes = inputStream.available();
        long bytesToSkip = availableBytes / 2;
        for (int i = 0; i < bytesToSkip; i++) {
            inputStream.read();
        }
    }
}
