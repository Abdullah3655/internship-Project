package com.recruitment.candidateservice.util;

import com.recruitment.candidateservice.exception.BadRequestException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class TagNames {

    private static final Pattern VALID = Pattern.compile("^[a-z][a-z0-9-]{1,31}$");
    private static final int MAX_TAGS = 12;

    private TagNames() {
    }

    public static List<String> normalize(List<String> rawTags) {
        if (rawTags == null || rawTags.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String raw : rawTags) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String name = raw.trim().toLowerCase(Locale.ROOT);
            if (!VALID.matcher(name).matches()) {
                throw new BadRequestException(
                        "Invalid tag '" + raw.trim() + "'. Use tags like java, spring "
                                + "(lowercase letters, digits, hyphens; 2–32 characters)."
                );
            }
            unique.add(name);
            if (unique.size() > MAX_TAGS) {
                throw new BadRequestException("At most " + MAX_TAGS + " tags are allowed");
            }
        }
        return new ArrayList<>(unique);
    }
}
