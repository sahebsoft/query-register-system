package com.balsam.oasis.common.registry.engine.sql;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SqlUtils {

    private static final Pattern BIND_PARAM_PATTERN = Pattern.compile(":(\\w+)");

    public static String replacePlaceholder(String sql, String placeholder, String replacement) {
        return sql.replace("--" + placeholder, replacement != null ? replacement : "");
    }

    public static Map<String, Object> extractBindParams(String sql, Map<String, Object> allParams) {
        Map<String, Object> bindParams = new HashMap<>();
        Matcher matcher = BIND_PARAM_PATTERN.matcher(sql);
        while (matcher.find()) {
            String paramName = matcher.group(1);
            if (allParams.containsKey(paramName)) {
                bindParams.put(paramName, allParams.get(paramName));
            }
        }
        return bindParams;
    }

    public static String wrapForCount(String sql) {
        return "SELECT COUNT(*) FROM (" + sql + ") count_query";
    }

    public static String cleanPlaceholders(String sql) {
        return sql.replaceAll("--\\w+", "");
    }

    public static boolean hasBindParameter(String sql, String paramName) {
        Pattern pattern = Pattern.compile(":" + Pattern.quote(paramName) + "\\b");
        return pattern.matcher(sql).find();
    }

}