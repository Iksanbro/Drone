package io.github.iksanbro.smartlogistics.frogport;

import java.util.ArrayList;
import java.util.List;

/**
 * Several addresses in the one filter string Create keeps per frogport. Create compares addresses
 * with {@code PackageItem.matchAddress}; a mixin there splits strings made here, so the chain
 * conveyor routing and the frogport's own checks see a port with several addresses.
 */
public final class AddressList {
    private AddressList() {}

    /** Starts every combined filter; a character nobody types into an address field. */
    public static final char MARK = '\u0001';
    public static final int MAX = 8;

    public static String encode(List<String> addresses) {
        List<String> clean = clean(addresses);
        if (clean.isEmpty()) return "";
        if (clean.size() == 1) return clean.get(0);
        StringBuilder sb = new StringBuilder();
        for (String a : clean) sb.append(MARK).append(a);
        return sb.toString();
    }

    public static boolean isCombined(String filter) {
        return filter != null && !filter.isEmpty() && filter.charAt(0) == MARK;
    }

    public static List<String> decode(String filter) {
        List<String> out = new ArrayList<>();
        if (filter == null || filter.isEmpty()) return out;
        if (!isCombined(filter)) {
            out.add(filter);
            return out;
        }
        for (String part : filter.substring(1).split(String.valueOf(MARK), -1))
            if (!part.isBlank()) out.add(part);
        return out;
    }

    public static List<String> clean(List<String> addresses) {
        List<String> out = new ArrayList<>();
        for (String a : addresses) {
            if (a == null) continue;
            String t = a.replace(String.valueOf(MARK), "").trim();
            if (!t.isEmpty() && !out.contains(t) && out.size() < MAX) out.add(t);
        }
        return out;
    }
}
