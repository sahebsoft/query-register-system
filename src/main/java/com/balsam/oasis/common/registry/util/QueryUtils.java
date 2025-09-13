package com.balsam.oasis.common.registry.util;

import com.balsam.oasis.common.registry.builder.QueryDefinitionBuilder;
import com.balsam.oasis.common.registry.domain.common.QueryResult;
import com.balsam.oasis.common.registry.domain.definition.*;
import com.balsam.oasis.common.registry.domain.exception.QueryException;
import com.balsam.oasis.common.registry.domain.execution.QueryContext;
import com.balsam.oasis.common.registry.domain.execution.QueryContext.Filter;
import com.balsam.oasis.common.registry.engine.query.QueryRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class QueryUtils {
    private static final Logger log = LoggerFactory.getLogger(QueryUtils.class);
    private static final Pattern BIND_PARAM_PATTERN = Pattern.compile(":(\\w+)");
    private static final Pattern NAMED_PARAM_PATTERN = Pattern.compile(":([a-zA-Z_][a-zA-Z0-9_]*)");
    private static final Map<Integer, Class<?>> SQL_TYPE_MAP = new HashMap<>();
    private static final Map<String, Class<?>> TYPE_NAME_MAP = new HashMap<>();

    static {
        initializeTypeMappings();
    }

    // ============= SQL BUILDING METHODS =============

    public static String replacePlaceholder(String sql, String placeholder, String replacement) {
        return sql.replace("--" + placeholder, replacement != null ? replacement : "");
    }

    public static String cleanPlaceholders(String sql) {
        return sql.replaceAll("--\\w+", "");
    }

    public static String wrapForCount(String sql) {
        return "SELECT COUNT(*) FROM (" + sql + ") count_query";
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

    public static boolean hasBindParameter(String sql, String paramName) {
        Pattern pattern = Pattern.compile(":" + Pattern.quote(paramName) + "\\b");
        return pattern.matcher(sql).find();
    }

    public static String applyFilters(String sql, QueryContext context, Map<String, Object> params) {
        if (context.getFilters() == null || context.getFilters().isEmpty()) {
            return sql;
        }

        StringBuilder filterClause = new StringBuilder();
        int paramIndex = 0;
        for (Filter filter : context.getFilters().values()) {
            AttributeDef<?> attr = context.getDefinition().getAttribute(filter.getAttribute());
            if (attr == null || !attr.filterable()) {
                log.warn("Attribute {} is not filterable or does not exist", filter.getAttribute());
                continue;
            }
            if (filterClause.length() > 0) {
                filterClause.append(" AND ");
            }
            String condition = buildFilterCondition(filter, attr, params, paramIndex++);
            filterClause.append(condition);
        }

        if (filterClause.length() > 0) {
            sql = "SELECT * FROM (" + sql + ") WHERE " + filterClause.toString();
        }
        return sql;
    }

    private static String buildFilterCondition(Filter filter, AttributeDef<?> attr,
            Map<String, Object> params, int index) {
        String column = attr.aliasName() != null ? attr.aliasName() : attr.name();
        String paramName = "filter_" + filter.getAttribute() + "_" + index;

        switch (filter.getOperator()) {
            case EQUALS:
                params.put(paramName, filter.getValue());
                return column + " = :" + paramName;
            case NOT_EQUALS:
                params.put(paramName, filter.getValue());
                return column + " != :" + paramName;
            case LIKE:
                params.put(paramName, filter.getValue());
                return "UPPER(" + column + ") LIKE UPPER(:" + paramName + ")";
            case NOT_LIKE:
                params.put(paramName, filter.getValue());
                return "UPPER(" + column + ") NOT LIKE UPPER(:" + paramName + ")";
            case IN:
                params.put(paramName, filter.getValues() != null ? filter.getValues() : filter.getValue());
                return column + " IN (:" + paramName + ")";
            case NOT_IN:
                params.put(paramName, filter.getValues() != null ? filter.getValues() : filter.getValue());
                return column + " NOT IN (:" + paramName + ")";
            case GREATER_THAN:
                params.put(paramName, filter.getValue());
                return column + " > :" + paramName;
            case GREATER_THAN_OR_EQUAL:
                params.put(paramName, filter.getValue());
                return column + " >= :" + paramName;
            case LESS_THAN:
                params.put(paramName, filter.getValue());
                return column + " < :" + paramName;
            case LESS_THAN_OR_EQUAL:
                params.put(paramName, filter.getValue());
                return column + " <= :" + paramName;
            case BETWEEN:
                params.put(paramName + "_1", filter.getValue());
                params.put(paramName + "_2", filter.getValue2());
                return column + " BETWEEN :" + paramName + "_1 AND :" + paramName + "_2";
            case IS_NULL:
                return column + " IS NULL";
            case IS_NOT_NULL:
                return column + " IS NOT NULL";
            default:
                throw new QueryException(QueryException.ErrorCode.PARAMETER_ERROR,
                    "Unsupported filter operator: " + filter.getOperator());
        }
    }

    public static String applySorting(String sql, QueryContext context) {
        if (context.getSorts() == null || context.getSorts().isEmpty()) {
            return sql;
        }

        String orderByClause = context.getSorts().stream()
            .map(sort -> {
                AttributeDef<?> attr = context.getDefinition().getAttribute(sort.getAttribute());
                if (attr == null || !attr.sortable()) {
                    log.warn("Attribute {} is not sortable or does not exist", sort.getAttribute());
                    return null;
                }
                String column = attr.aliasName() != null ? attr.aliasName() : attr.name();
                return column + " " + sort.getDirection().name();
            })
            .filter(s -> s != null)
            .collect(Collectors.joining(", "));

        if (!orderByClause.isEmpty()) {
            sql = replacePlaceholder(sql, "orderBy", "ORDER BY " + orderByClause);
        }
        return sql;
    }

    public static String applyCriteria(String sql, QueryContext context, Map<String, Object> params) {
        QueryDefinitionBuilder definition = context.getDefinition();
        if (definition.getCriteria() == null || definition.getCriteria().isEmpty()) {
            return sql;
        }

        for (Map.Entry<String, CriteriaDef> entry : definition.getCriteria().entrySet()) {
            CriteriaDef criteria = entry.getValue();
            if (criteria.condition() == null || criteria.condition().test(context)) {
                String placeholder = entry.getKey();
                String sqlFragment = criteria.sql();
                sql = replacePlaceholder(sql, placeholder, sqlFragment);
            }
        }
        return sql;
    }

    public static String applyPagination(String sql, QueryContext context) {
        if (context.getPagination() == null) {
            return sql;
        }
        int offset = context.getPagination().getOffset();
        Integer limit = context.getPagination().getLimit();
        return applyOracle11gPagination(sql, offset, limit);
    }

    private static String applyOracle11gPagination(String sql, int offset, Integer limit) {
        if (limit == null) {
            return sql;
        }
        StringBuilder paginated = new StringBuilder();
        paginated.append("SELECT * FROM (");
        paginated.append("SELECT query_.*, ROWNUM rnum_ FROM (");
        paginated.append(sql);
        paginated.append(") query_ WHERE ROWNUM <= ").append(offset + limit);
        paginated.append(") WHERE rnum_ > ").append(offset);
        return paginated.toString();
    }

    // ============= TYPE CONVERSION METHODS =============

    private static void initializeTypeMappings() {
        SQL_TYPE_MAP.put(Types.TINYINT, Byte.class);
        SQL_TYPE_MAP.put(Types.SMALLINT, Short.class);
        SQL_TYPE_MAP.put(Types.INTEGER, Integer.class);
        SQL_TYPE_MAP.put(Types.BIGINT, Long.class);
        SQL_TYPE_MAP.put(Types.FLOAT, Float.class);
        SQL_TYPE_MAP.put(Types.REAL, Float.class);
        SQL_TYPE_MAP.put(Types.DOUBLE, Double.class);
        SQL_TYPE_MAP.put(Types.NUMERIC, BigDecimal.class);
        SQL_TYPE_MAP.put(Types.DECIMAL, BigDecimal.class);
        SQL_TYPE_MAP.put(Types.CHAR, String.class);
        SQL_TYPE_MAP.put(Types.VARCHAR, String.class);
        SQL_TYPE_MAP.put(Types.LONGVARCHAR, String.class);
        SQL_TYPE_MAP.put(Types.NCHAR, String.class);
        SQL_TYPE_MAP.put(Types.NVARCHAR, String.class);
        SQL_TYPE_MAP.put(Types.LONGNVARCHAR, String.class);
        SQL_TYPE_MAP.put(Types.CLOB, String.class);
        SQL_TYPE_MAP.put(Types.NCLOB, String.class);
        SQL_TYPE_MAP.put(Types.DATE, Date.class);
        SQL_TYPE_MAP.put(Types.TIME, Time.class);
        SQL_TYPE_MAP.put(Types.TIMESTAMP, Timestamp.class);
        SQL_TYPE_MAP.put(Types.TIME_WITH_TIMEZONE, Time.class);
        SQL_TYPE_MAP.put(Types.TIMESTAMP_WITH_TIMEZONE, Timestamp.class);
        SQL_TYPE_MAP.put(Types.BOOLEAN, Boolean.class);
        SQL_TYPE_MAP.put(Types.BIT, Boolean.class);
        SQL_TYPE_MAP.put(Types.BINARY, byte[].class);
        SQL_TYPE_MAP.put(Types.VARBINARY, byte[].class);
        SQL_TYPE_MAP.put(Types.LONGVARBINARY, byte[].class);
        SQL_TYPE_MAP.put(Types.BLOB, byte[].class);

        TYPE_NAME_MAP.put("NUMBER", BigDecimal.class);
        TYPE_NAME_MAP.put("VARCHAR2", String.class);
        TYPE_NAME_MAP.put("CHAR", String.class);
        TYPE_NAME_MAP.put("DATE", Timestamp.class);
        TYPE_NAME_MAP.put("TIMESTAMP", Timestamp.class);
        TYPE_NAME_MAP.put("CLOB", String.class);
        TYPE_NAME_MAP.put("BLOB", byte[].class);
    }

    public static Class<?> getJavaType(int sqlType) {
        Class<?> javaType = SQL_TYPE_MAP.get(sqlType);
        if (javaType == null) {
            log.warn("Unknown SQL type: {}, defaulting to Object", sqlType);
            return Object.class;
        }
        return javaType;
    }

    public static Class<?> getJavaType(String typeName) {
        if (typeName == null) {
            return Object.class;
        }
        String upperTypeName = typeName.toUpperCase();
        Class<?> javaType = TYPE_NAME_MAP.get(upperTypeName);
        if (javaType == null) {
            try {
                JDBCType jdbcType = JDBCType.valueOf(upperTypeName);
                javaType = getJavaType(jdbcType.getVendorTypeNumber());
            } catch (IllegalArgumentException e) {
                log.warn("Unknown type name: {}, defaulting to Object", typeName);
                return Object.class;
            }
        }
        return javaType;
    }

    @SuppressWarnings("unchecked")
    public static <T> T convertValue(Object value, Class<T> targetType) {
        if (value == null) {
            return null;
        }
        if (targetType.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        if (targetType == String.class) {
            return (T) value.toString();
        }
        if (Number.class.isAssignableFrom(targetType)) {
            return convertToNumber(value, targetType);
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return (T) convertToBoolean(value);
        }
        if (Date.class.isAssignableFrom(targetType)) {
            return convertToDate(value, targetType);
        }
        if (LocalDate.class.isAssignableFrom(targetType)) {
            return (T) convertToLocalDate(value);
        }
        if (LocalDateTime.class.isAssignableFrom(targetType)) {
            return (T) convertToLocalDateTime(value);
        }
        if (LocalTime.class.isAssignableFrom(targetType)) {
            return (T) convertToLocalTime(value);
        }
        try {
            return targetType.cast(value);
        } catch (ClassCastException e) {
            log.warn("Cannot convert {} to {}", value.getClass(), targetType);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T convertToNumber(Object value, Class<T> targetType) {
        Number number;
        if (value instanceof Number) {
            number = (Number) value;
        } else if (value instanceof String) {
            try {
                number = new BigDecimal(value.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            return null;
        }

        if (targetType == Byte.class || targetType == byte.class) {
            return (T) Byte.valueOf(number.byteValue());
        } else if (targetType == Short.class || targetType == short.class) {
            return (T) Short.valueOf(number.shortValue());
        } else if (targetType == Integer.class || targetType == int.class) {
            return (T) Integer.valueOf(number.intValue());
        } else if (targetType == Long.class || targetType == long.class) {
            return (T) Long.valueOf(number.longValue());
        } else if (targetType == Float.class || targetType == float.class) {
            return (T) Float.valueOf(number.floatValue());
        } else if (targetType == Double.class || targetType == double.class) {
            return (T) Double.valueOf(number.doubleValue());
        } else if (targetType == BigDecimal.class) {
            if (value instanceof BigDecimal) {
                return (T) value;
            }
            return (T) new BigDecimal(number.toString());
        }
        return null;
    }

    private static Boolean convertToBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        } else if (value instanceof String) {
            String str = value.toString().toLowerCase();
            return "true".equals(str) || "yes".equals(str) || "y".equals(str) || "1".equals(str);
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T> T convertToDate(Object value, Class<T> targetType) {
        if (value == null) {
            return null;
        }
        long millis;
        if (value instanceof java.util.Date) {
            millis = ((java.util.Date) value).getTime();
        } else if (value instanceof Number) {
            millis = ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                millis = Timestamp.valueOf(value.toString()).getTime();
            } catch (IllegalArgumentException e) {
                return null;
            }
        } else {
            return null;
        }

        if (targetType == Date.class) {
            return (T) new Date(millis);
        } else if (targetType == Time.class) {
            return (T) new Time(millis);
        } else if (targetType == Timestamp.class) {
            return (T) new Timestamp(millis);
        } else if (targetType == java.util.Date.class) {
            return (T) new java.util.Date(millis);
        }
        return null;
    }

    private static LocalDate convertToLocalDate(Object value) {
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        } else if (value instanceof Date) {
            return ((Date) value).toLocalDate();
        } else if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime().toLocalDate();
        } else if (value instanceof String) {
            return LocalDate.parse(value.toString());
        }
        return null;
    }

    private static LocalDateTime convertToLocalDateTime(Object value) {
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        } else if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        } else if (value instanceof Date) {
            return new Timestamp(((Date) value).getTime()).toLocalDateTime();
        } else if (value instanceof String) {
            return LocalDateTime.parse(value.toString());
        }
        return null;
    }

    private static LocalTime convertToLocalTime(Object value) {
        if (value instanceof LocalTime) {
            return (LocalTime) value;
        } else if (value instanceof Time) {
            return ((Time) value).toLocalTime();
        } else if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime().toLocalTime();
        } else if (value instanceof String) {
            return LocalTime.parse(value.toString());
        }
        return null;
    }

    public static Class<?> extractJavaType(ResultSetMetaData metaData, int columnIndex) throws SQLException {
        int sqlType = metaData.getColumnType(columnIndex);
        String typeName = metaData.getColumnTypeName(columnIndex);
        Class<?> javaType = getJavaType(typeName);
        if (javaType != Object.class) {
            return javaType;
        }
        return getJavaType(sqlType);
    }

    public static Object getResultSetValue(ResultSet rs, int columnIndex, Class<?> targetType) throws SQLException {
        Object value = rs.getObject(columnIndex);
        if (value == null || rs.wasNull()) {
            return null;
        }
        if (targetType != null && targetType != Object.class) {
            return convertValue(value, targetType);
        }
        return value;
    }

    // ============= VALIDATION METHODS =============

    public static void validateQuery(QueryDefinitionBuilder queryDef) {
        String queryName = queryDef.getName();
        Set<String> sqlBindParams = extractBindParameters(queryDef.getSql());
        Set<String> criteriaBindParams = new HashSet<>();

        if (queryDef.getCriteria() != null) {
            for (Map.Entry<String, CriteriaDef> entry : queryDef.getCriteria().entrySet()) {
                CriteriaDef criteria = entry.getValue();
                if (criteria.sql() != null) {
                    Set<String> params = extractBindParameters(criteria.sql());
                    criteriaBindParams.addAll(params);
                }
            }
        }

        Set<String> allBindParams = new HashSet<>();
        allBindParams.addAll(sqlBindParams);
        allBindParams.addAll(criteriaBindParams);

        Set<String> definedParams = new HashSet<>();
        if (queryDef.getParameters() != null) {
            definedParams.addAll(queryDef.getParameters().keySet());
        }

        Set<String> systemParams = Set.of("offset", "limit", "_start", "_end");

        Set<String> undefinedParams = new HashSet<>(allBindParams);
        undefinedParams.removeAll(definedParams);
        undefinedParams.removeAll(systemParams);

        Set<String> filterGeneratedParams = new HashSet<>();
        if (queryDef.getAttributes() != null) {
            for (String attrName : queryDef.getAttributes().keySet()) {
                filterGeneratedParams.add(attrName);
                filterGeneratedParams.add(attrName + "_eq");
                filterGeneratedParams.add(attrName + "_ne");
                filterGeneratedParams.add(attrName + "_gt");
                filterGeneratedParams.add(attrName + "_gte");
                filterGeneratedParams.add(attrName + "_lt");
                filterGeneratedParams.add(attrName + "_lte");
                filterGeneratedParams.add(attrName + "_like");
                filterGeneratedParams.add(attrName + "_in");
                filterGeneratedParams.add(attrName + "_between_1");
                filterGeneratedParams.add(attrName + "_between_2");
                filterGeneratedParams.add(attrName + "_null");
                filterGeneratedParams.add(attrName + "_notnull");
            }
        }
        undefinedParams.removeAll(filterGeneratedParams);

        if (!undefinedParams.isEmpty()) {
            String errorMsg = String.format("""
                    Query '%s' uses undefined bind parameters: %s
                    Defined parameters: %s
                    Used in SQL: %s
                    Used in criteria: %s
                    Make sure all :paramName references have corresponding ParamDef definitions.""",
                    queryName, undefinedParams, definedParams, sqlBindParams, criteriaBindParams);
            throw new IllegalStateException(errorMsg);
        }
    }

    public static Set<String> findUnusedParameters(QueryDefinitionBuilder queryDef) {
        Set<String> sqlBindParams = extractBindParameters(queryDef.getSql());
        Set<String> criteriaBindParams = new HashSet<>();

        if (queryDef.getCriteria() != null) {
            for (Map.Entry<String, CriteriaDef> entry : queryDef.getCriteria().entrySet()) {
                CriteriaDef criteria = entry.getValue();
                if (criteria.sql() != null) {
                    Set<String> params = extractBindParameters(criteria.sql());
                    criteriaBindParams.addAll(params);
                }
            }
        }

        Set<String> allBindParams = new HashSet<>();
        allBindParams.addAll(sqlBindParams);
        allBindParams.addAll(criteriaBindParams);

        Set<String> definedParams = new HashSet<>();
        if (queryDef.getParameters() != null) {
            definedParams.addAll(queryDef.getParameters().keySet());
        }

        Set<String> systemParams = Set.of("offset", "limit", "_start", "_end");

        Set<String> unusedParams = new HashSet<>(definedParams);
        unusedParams.removeAll(allBindParams);
        unusedParams.removeAll(systemParams);
        return unusedParams;
    }

    public static Set<String> extractBindParameters(String sql) {
        Set<String> params = new HashSet<>();
        if (sql == null || sql.isEmpty()) {
            return params;
        }
        Matcher matcher = NAMED_PARAM_PATTERN.matcher(sql);
        while (matcher.find()) {
            params.add(matcher.group(1));
        }
        return params;
    }
}