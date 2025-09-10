package com.balsam.oasis.common.registry.engine.sql.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.balsam.oasis.common.registry.domain.common.AppliedCriteria;
import com.balsam.oasis.common.registry.domain.definition.CriteriaDef;
import com.balsam.oasis.common.registry.domain.execution.QueryContext;

public class CriteriaUtils {

    private static final Pattern BIND_PARAM_PATTERN = Pattern.compile(":(\\w+)");

    public static String applyCriteria(String sql,
            Map<String, CriteriaDef> criteria,
            QueryContext context,
            Map<String, Object> params,
            boolean trackApplied) {
        if (criteria == null || criteria.isEmpty()) {
            return sql;
        }

        for (CriteriaDef criteriaDef : criteria.values()) {
            String placeholder = "--" + criteriaDef.name();
            if (sql.contains(placeholder)) {
                boolean shouldApply = shouldApplyCriteria(criteriaDef, context, params);

                if (shouldApply) {
                    sql = sql.replace(placeholder, criteriaDef.sql());

                    if (trackApplied) {
                        context.addAppliedCriteria(
                                AppliedCriteria.builder()
                                        .name(criteriaDef.name())
                                        .sql(criteriaDef.sql())
                                        .params(SqlUtils.extractBindParams(criteriaDef.sql(), params))
                                        .build());
                    }
                } else {
                    sql = sql.replace(placeholder, "");
                }
            }
        }

        return sql;
    }

    public static boolean shouldApplyCriteria(CriteriaDef criteria,
            QueryContext context,
            Map<String, Object> params) {
        if (criteria.condition() != null) {
            // The condition predicate expects QueryContext, but we have BaseContext
            // This is safe because the condition will only be used with appropriate context
            // types
            return criteria.condition().test(context);
        }

        String criteriaSql = criteria.sql();
        if (criteriaSql != null) {
            Matcher matcher = BIND_PARAM_PATTERN.matcher(criteriaSql);
            while (matcher.find()) {
                String paramName = matcher.group(1);
                if (!params.containsKey(paramName) || params.get(paramName) == null) {
                    return false;
                }
            }
        }

        return true;
    }

    public static String buildCriteriaPlaceholder(String criteriaName) {
        return "--" + criteriaName;
    }

    public static boolean hasCriteriaPlaceholder(String sql, String criteriaName) {
        return sql.contains(buildCriteriaPlaceholder(criteriaName));
    }
}