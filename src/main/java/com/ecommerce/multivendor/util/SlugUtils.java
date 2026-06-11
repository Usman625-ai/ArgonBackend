package com.ecommerce.multivendor.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public class SlugUtils {
    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern DASHES = Pattern.compile("-{2,}");

    public static String toSlug(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String slug = normalized.toLowerCase(Locale.ENGLISH);
        slug = WHITESPACE.matcher(slug).replaceAll("-");
        slug = NON_LATIN.matcher(slug).replaceAll("");
        slug = DASHES.matcher(slug).replaceAll("-");
        return slug.replaceAll("^-|-$", "");
    }

    private SlugUtils() {}
}
