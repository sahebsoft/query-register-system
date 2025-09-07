package com.balsam.oasis.common.registry.processor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.balsam.oasis.common.registry.base.BaseContext;
import com.balsam.oasis.common.registry.core.result.Row;

/**
 * Comprehensive test coverage for Calculator interface and implementations.
 * Tests all calculation scenarios, row data access, context usage, and edge
 * cases.
 */
class CalculatorTest {

    // ========== BASIC FUNCTIONAL INTERFACE TESTS ==========

    @Test
    void testBasicCalculator_StringConcatenation() {
        Calculator<String> nameCalculator = (row, context) -> {
            String firstName = row.getString("firstName");
            String lastName = row.getString("lastName");
            return firstName + " " + lastName;
        };

        Row row = mockRow(Map.of(
                "firstName", "John",
                "lastName", "Doe"));
        BaseContext<?> context = mockContext();

        String result = nameCalculator.calculate(row, context);
        assertThat(result).isEqualTo("John Doe");
    }

    @Test
    void testBasicCalculator_NumericalCalculation() {
        Calculator<BigDecimal> totalCalculator = (row, context) -> {
            BigDecimal price = row.getBigDecimal("price");
            Integer quantity = row.getInteger("quantity");
            if (price == null || quantity == null)
                return BigDecimal.ZERO;
            return price.multiply(new BigDecimal(quantity));
        };

        Row row = mockRow(Map.of(
                "price", new BigDecimal("10.50"),
                "quantity", 3));
        BaseContext<?> context = mockContext();

        BigDecimal result = totalCalculator.calculate(row, context);
        assertThat(result).isEqualTo(new BigDecimal("31.50"));
    }

    @Test
    void testBasicCalculator_BooleanLogic() {
        Calculator<Boolean> isEligibleCalculator = (row, context) -> {
            Integer age = row.getInteger("age");
            String status = row.getString("status");
            return age != null && age >= 18 && "ACTIVE".equals(status);
        };

        Row row = mockRow(Map.of(
                "age", 25,
                "status", "ACTIVE"));
        BaseContext<?> context = mockContext();

        Boolean result = isEligibleCalculator.calculate(row, context);
        assertThat(result).isTrue();
    }

    // ========== NULL HANDLING TESTS ==========

    @Test
    void testCalculator_WithNullRowValues() {
        Calculator<String> safeCalculator = (row, context) -> {
            String firstName = row.getString("firstName");
            String lastName = row.getString("lastName");
            if (firstName == null)
                firstName = "Unknown";
            if (lastName == null)
                lastName = "User";
            return firstName + " " + lastName;
        };

        Row row = mockRow(Map.of(
                "lastName", "Doe" // Only include non-null values
        ));
        BaseContext<?> context = mockContext();

        String result = safeCalculator.calculate(row, context);
        assertThat(result).isEqualTo("Unknown Doe");
    }

    @Test
    void testCalculator_WithNullRow() {
        Calculator<String> calculator = (row, context) -> {
            if (row == null)
                return "No data";
            return row.getString("name", "Default");
        };

        BaseContext<?> context = mockContext();

        String result = calculator.calculate(null, context);
        assertThat(result).isEqualTo("No data");
    }

    @Test
    void testCalculator_WithNullContext() {
        Calculator<String> calculator = (row, context) -> {
            String name = row.getString("name");
            if (context == null)
                return name + " (no context)";
            return name + " (with context)";
        };

        Row row = mockRow(Map.of("name", "John"));

        String result = calculator.calculate(row, null);
        assertThat(result).isEqualTo("John (no context)");
    }

    // ========== ROW DATA ACCESS TESTS ==========

    @Test
    void testCalculator_AccessAllDataTypes() {
        Calculator<Map<String, Object>> dataExtractor = (row, context) -> {
            Map<String, Object> result = new HashMap<>();
            result.put("string", row.getString("name"));
            result.put("integer", row.getInteger("age"));
            result.put("long", row.getLong("id"));
            result.put("double", row.getDouble("score"));
            result.put("bigDecimal", row.getBigDecimal("salary"));
            result.put("localDate", row.getLocalDate("birthDate"));
            result.put("localDateTime", row.getLocalDateTime("lastLogin"));
            result.put("boolean", row.getBoolean("active"));
            return result;
        };

        LocalDate birthDate = LocalDate.of(1990, 5, 15);
        LocalDateTime lastLogin = LocalDateTime.of(2024, 12, 20, 14, 30);

        Row row = mockRow(Map.of(
                "name", "John Doe",
                "age", 34,
                "id", 123456L,
                "score", 95.5,
                "salary", new BigDecimal("75000.00"),
                "birthDate", birthDate,
                "lastLogin", lastLogin,
                "active", true));
        BaseContext<?> context = mockContext();

        Map<String, Object> result = dataExtractor.calculate(row, context);

        assertThat(result.get("string")).isEqualTo("John Doe");
        assertThat(result.get("integer")).isEqualTo(34);
        assertThat(result.get("long")).isEqualTo(123456L);
        assertThat(result.get("double")).isEqualTo(95.5);
        assertThat(result.get("bigDecimal")).isEqualTo(new BigDecimal("75000.00"));
        assertThat(result.get("localDate")).isEqualTo(birthDate);
        assertThat(result.get("localDateTime")).isEqualTo(lastLogin);
        assertThat(result.get("boolean")).isEqualTo(true);
    }

    @Test
    void testCalculator_WithDefaultValues() {
        Calculator<String> formattedCalculator = (row, context) -> {
            String name = row.getString("name", "Anonymous");
            Integer age = row.getInteger("age", 0);
            Boolean active = row.getBoolean("active", false);
            return String.format("%s (%d) - %s", name, age, active ? "Active" : "Inactive");
        };

        Row row = mockRow(Map.of()); // Empty map - all getters will return null
        BaseContext<?> context = mockContext();

        String result = formattedCalculator.calculate(row, context);
        assertThat(result).isEqualTo("Anonymous (0) - Inactive");
    }

    // ========== CONTEXT USAGE TESTS ==========

    @Test
    void testCalculator_AccessContextParameters() {
        Calculator<String> paramCalculator = (row, context) -> {
            String name = row.getString("name");
            String prefix = (String) context.getParam("prefix");
            String suffix = (String) context.getParam("suffix");
            return (prefix != null ? prefix : "") + name + (suffix != null ? suffix : "");
        };

        Row row = mockRow(Map.of("name", "John"));
        BaseContext<?> context = mockContextWithParams(Map.of(
                "prefix", "Mr. ",
                "suffix", " Jr."));

        String result = paramCalculator.calculate(row, context);
        assertThat(result).isEqualTo("Mr. John Jr.");
    }

    @Test
    void testCalculator_AccessContextMetadata() {
        Calculator<String> metadataCalculator = (row, context) -> {
            String name = row.getString("name");
            String department = (String) context.getMetadata().get("department");
            String company = (String) context.getMetadata().get("company");
            return String.format("%s from %s at %s", name, department, company);
        };

        Row row = mockRow(Map.of("name", "John"));
        BaseContext<?> context = mockContext();
        context.addMetadata("department", "Engineering");
        context.addMetadata("company", "TechCorp");

        String result = metadataCalculator.calculate(row, context);
        assertThat(result).isEqualTo("John from Engineering at TechCorp");
    }

    @Test
    void testCalculator_AccessExecutionTiming() {
        Calculator<String> timingCalculator = (row, context) -> {
            String name = row.getString("name");
            long executionTime = context.getExecutionTime();
            return String.format("%s (processed in %dms)", name, executionTime);
        };

        Row row = mockRow(Map.of("name", "John"));
        BaseContext<?> context = mockContext();
        context.startExecution();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            /* ignore */ }
        context.endExecution();

        String result = timingCalculator.calculate(row, context);
        assertThat(result).startsWith("John (processed in ");
        assertThat(result).endsWith("ms)");
    }

    // ========== COMPLEX CALCULATION TESTS ==========

    @Test
    void testCalculator_CompensationCalculation() {
        Calculator<BigDecimal> compensationCalculator = (row, context) -> {
            BigDecimal salary = row.getBigDecimal("salary");
            BigDecimal commission = row.getBigDecimal("commissionPct");
            if (salary == null)
                return BigDecimal.ZERO;
            if (commission == null)
                return salary;
            return salary.add(salary.multiply(commission));
        };

        Row row = mockRow(Map.of(
                "salary", new BigDecimal("50000.00"),
                "commissionPct", new BigDecimal("0.15")));
        BaseContext<?> context = mockContext();

        BigDecimal result = compensationCalculator.calculate(row, context);
        assertThat(result).isEqualByComparingTo(new BigDecimal("57500.00"));
    }

    @Test
    void testCalculator_AgeCalculation() {
        Calculator<Integer> ageCalculator = (row, context) -> {
            LocalDate birthDate = row.getLocalDate("birthDate");
            if (birthDate == null)
                return null;
            return (int) ChronoUnit.YEARS.between(birthDate, LocalDate.now());
        };

        LocalDate birthDate = LocalDate.now().minusYears(30).minusMonths(6);
        Row row = mockRow(Map.of("birthDate", birthDate));
        BaseContext<?> context = mockContext();

        Integer result = ageCalculator.calculate(row, context);
        assertThat(result).isEqualTo(30);
    }

    @Test
    void testCalculator_PercentageCalculation() {
        Calculator<Double> percentageCalculator = (row, context) -> {
            Double current = row.getDouble("current");
            Double total = row.getDouble("total");
            if (current == null || total == null || total == 0)
                return 0.0;
            return (current / total) * 100.0;
        };

        Row row = mockRow(Map.of(
                "current", 75.0,
                "total", 150.0));
        BaseContext<?> context = mockContext();

        Double result = percentageCalculator.calculate(row, context);
        assertThat(result).isEqualTo(50.0);
    }

    @Test
    void testCalculator_ConditionalLogic() {
        Calculator<String> gradeCalculator = (row, context) -> {
            Integer score = row.getInteger("score");
            if (score == null)
                return "N/A";
            if (score >= 90)
                return "A";
            if (score >= 80)
                return "B";
            if (score >= 70)
                return "C";
            if (score >= 60)
                return "D";
            return "F";
        };

        Row row = mockRow(Map.of("score", 85));
        BaseContext<?> context = mockContext();

        String result = gradeCalculator.calculate(row, context);
        assertThat(result).isEqualTo("B");
    }

    // ========== MATHEMATICAL OPERATIONS TESTS ==========

    @ParameterizedTest
    @CsvSource({
            "100.0, 1.08, 108.00",
            "50.0, 1.15, 57.50",
            "0.0, 1.10, 0.00",
            "1000.0, 1.00, 1000.00"
    })
    void testCalculator_TaxCalculation(double amount, double taxRate, double expected) {
        Calculator<BigDecimal> taxCalculator = (row, context) -> {
            BigDecimal baseAmount = row.getBigDecimal("amount");
            BigDecimal rate = row.getBigDecimal("taxRate");
            if (baseAmount == null || rate == null)
                return BigDecimal.ZERO;
            return baseAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        };

        Row row = mockRow(Map.of(
                "amount", new BigDecimal(amount),
                "taxRate", new BigDecimal(taxRate)));
        BaseContext<?> context = mockContext();

        BigDecimal result = taxCalculator.calculate(row, context);
        assertThat(result).isEqualTo(new BigDecimal(expected).setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    void testCalculator_StatisticalCalculation() {
        Calculator<Map<String, Double>> statsCalculator = (row, context) -> {
            Double value1 = row.getDouble("value1");
            Double value2 = row.getDouble("value2");
            Double value3 = row.getDouble("value3");

            if (value1 == null || value2 == null || value3 == null) {
                return Map.of("mean", 0.0, "min", 0.0, "max", 0.0);
            }

            double mean = (value1 + value2 + value3) / 3.0;
            double min = Math.min(Math.min(value1, value2), value3);
            double max = Math.max(Math.max(value1, value2), value3);

            return Map.of("mean", mean, "min", min, "max", max);
        };

        Row row = mockRow(Map.of(
                "value1", 10.0,
                "value2", 20.0,
                "value3", 30.0));
        BaseContext<?> context = mockContext();

        Map<String, Double> result = statsCalculator.calculate(row, context);
        assertThat(result.get("mean")).isEqualTo(20.0);
        assertThat(result.get("min")).isEqualTo(10.0);
        assertThat(result.get("max")).isEqualTo(30.0);
    }

    // ========== STRING MANIPULATION TESTS ==========

    @Test
    void testCalculator_StringFormatting() {
        Calculator<String> formatterCalculator = (row, context) -> {
            String name = row.getString("name");
            Integer age = row.getInteger("age");
            BigDecimal salary = row.getBigDecimal("salary");

            if (name == null)
                return "No data";

            return String.format("%s, age %d, earns $%,.2f",
                    name,
                    age != null ? age : 0,
                    salary != null ? salary : BigDecimal.ZERO);
        };

        Row row = mockRow(Map.of(
                "name", "John Doe",
                "age", 30,
                "salary", new BigDecimal("75000.50")));
        BaseContext<?> context = mockContext();

        String result = formatterCalculator.calculate(row, context);
        assertThat(result).isEqualTo("John Doe, age 30, earns $75,000.50");
    }

    @Test
    void testCalculator_StringConcatenationWithNulls() {
        Calculator<String> concatenationCalculator = (row, context) -> {
            String[] parts = {
                    row.getString("part1"),
                    row.getString("part2"),
                    row.getString("part3")
            };

            StringBuilder result = new StringBuilder();
            for (String part : parts) {
                if (part != null && !part.trim().isEmpty()) {
                    if (result.length() > 0)
                        result.append(" | ");
                    result.append(part);
                }
            }

            return result.length() > 0 ? result.toString() : "No content";
        };

        Row row = mockRow(Map.of(
                "part1", "First",
                "part3", "Third" // Skip part2 so it returns null
        ));
        BaseContext<?> context = mockContext();

        String result = concatenationCalculator.calculate(row, context);
        assertThat(result).isEqualTo("First | Third");
    }

    // ========== DATE AND TIME CALCULATIONS ==========

    @Test
    void testCalculator_DaysBetweenDates() {
        Calculator<Long> daysBetweenCalculator = (row, context) -> {
            LocalDate startDate = row.getLocalDate("startDate");
            LocalDate endDate = row.getLocalDate("endDate");
            if (startDate == null || endDate == null)
                return 0L;
            return ChronoUnit.DAYS.between(startDate, endDate);
        };

        Row row = mockRow(Map.of(
                "startDate", LocalDate.of(2024, 1, 1),
                "endDate", LocalDate.of(2024, 1, 31)));
        BaseContext<?> context = mockContext();

        Long result = daysBetweenCalculator.calculate(row, context);
        assertThat(result).isEqualTo(30L);
    }

    @Test
    void testCalculator_TimeFormatting() {
        Calculator<String> timeFormatterCalculator = (row, context) -> {
            LocalDateTime timestamp = row.getLocalDateTime("timestamp");
            if (timestamp == null)
                return "No timestamp";
            return timestamp.toLocalDate().toString() + " at " + timestamp.toLocalTime().toString();
        };

        LocalDateTime timestamp = LocalDateTime.of(2024, 12, 20, 14, 30, 45);
        Row row = mockRow(Map.of("timestamp", timestamp));
        BaseContext<?> context = mockContext();

        String result = timeFormatterCalculator.calculate(row, context);
        assertThat(result).isEqualTo("2024-12-20 at 14:30:45");
    }

    // ========== ERROR HANDLING TESTS ==========

    @Test
    void testCalculator_HandleDivisionByZero() {
        Calculator<Double> divisionCalculator = (row, context) -> {
            Double dividend = row.getDouble("dividend");
            Double divisor = row.getDouble("divisor");

            if (dividend == null || divisor == null)
                return null;
            if (divisor == 0.0)
                return Double.POSITIVE_INFINITY;

            return dividend / divisor;
        };

        Row row = mockRow(Map.of(
                "dividend", 10.0,
                "divisor", 0.0));
        BaseContext<?> context = mockContext();

        Double result = divisionCalculator.calculate(row, context);
        assertThat(result).isEqualTo(Double.POSITIVE_INFINITY);
    }

    @Test
    void testCalculator_HandleInvalidData() {
        Calculator<String> validationCalculator = (row, context) -> {
            try {
                Integer value = row.getInteger("value");
                if (value == null)
                    return "NULL_VALUE";
                if (value < 0)
                    return "NEGATIVE_VALUE";
                if (value > 100)
                    return "EXCEEDS_LIMIT";
                return "VALID: " + value;
            } catch (Exception e) {
                return "ERROR: " + e.getMessage();
            }
        };

        Row row = mockRow(Map.of("value", -5));
        BaseContext<?> context = mockContext();

        String result = validationCalculator.calculate(row, context);
        assertThat(result).isEqualTo("NEGATIVE_VALUE");
    }

    @Test
    void testCalculator_ExceptionPropagation() {
        Calculator<String> failingCalculator = (row, context) -> {
            throw new RuntimeException("Calculation failed");
        };

        Row row = mockRow(Map.of());
        BaseContext<?> context = mockContext();

        assertThatThrownBy(() -> failingCalculator.calculate(row, context))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Calculation failed");
    }

    // ========== COMPLEX REAL-WORLD SCENARIOS ==========

    @Test
    void testCalculator_EmployeeBonusCalculation() {
        Calculator<BigDecimal> bonusCalculator = (row, context) -> {
            BigDecimal salary = row.getBigDecimal("salary");
            Double performanceRating = row.getDouble("performanceRating");
            Integer yearsOfService = row.getInteger("yearsOfService");
            String department = row.getString("department");

            if (salary == null || performanceRating == null)
                return BigDecimal.ZERO;

            // Base bonus percentage based on performance
            BigDecimal bonusPercentage = BigDecimal.ZERO;
            if (performanceRating >= 4.5)
                bonusPercentage = new BigDecimal("0.15");
            else if (performanceRating >= 3.5)
                bonusPercentage = new BigDecimal("0.10");
            else if (performanceRating >= 2.5)
                bonusPercentage = new BigDecimal("0.05");

            // Department multiplier
            BigDecimal departmentMultiplier = new BigDecimal("1.0");
            if ("SALES".equals(department))
                departmentMultiplier = new BigDecimal("1.2");
            else if ("ENGINEERING".equals(department))
                departmentMultiplier = new BigDecimal("1.1");

            // Years of service bonus
            BigDecimal serviceBonus = BigDecimal.ZERO;
            if (yearsOfService != null && yearsOfService >= 5) {
                serviceBonus = new BigDecimal("0.02");
            }

            BigDecimal totalPercentage = bonusPercentage.add(serviceBonus).multiply(departmentMultiplier);
            return salary.multiply(totalPercentage).setScale(2, RoundingMode.HALF_UP);
        };

        Row row = mockRow(Map.of(
                "salary", new BigDecimal("80000.00"),
                "performanceRating", 4.2,
                "yearsOfService", 7,
                "department", "ENGINEERING"));
        BaseContext<?> context = mockContext();

        BigDecimal result = bonusCalculator.calculate(row, context);
        // (10% + 2%) * 1.1 * 80000 = 12% * 1.1 * 80000 = 10,560
        assertThat(result).isEqualTo(new BigDecimal("10560.00"));
    }

    @Test
    void testCalculator_InventoryStatusCalculation() {
        Calculator<String> statusCalculator = (row, context) -> {
            Integer currentStock = row.getInteger("currentStock");
            Integer minThreshold = row.getInteger("minThreshold");
            Integer maxCapacity = row.getInteger("maxCapacity");
            LocalDate lastReorder = row.getLocalDate("lastReorder");

            if (currentStock == null)
                return "UNKNOWN";

            // Check stock levels
            if (currentStock == 0)
                return "OUT_OF_STOCK";
            if (minThreshold != null && currentStock < minThreshold)
                return "LOW_STOCK";
            if (maxCapacity != null && currentStock > maxCapacity)
                return "OVERSTOCKED";

            // Check reorder timing
            if (lastReorder != null) {
                long daysSinceReorder = ChronoUnit.DAYS.between(lastReorder, LocalDate.now());
                if (daysSinceReorder > 90)
                    return "STALE_INVENTORY";
            }

            return "OPTIMAL";
        };

        Row row = mockRow(Map.of(
                "currentStock", 25,
                "minThreshold", 10,
                "maxCapacity", 100,
                "lastReorder", LocalDate.now().minusDays(30)));
        BaseContext<?> context = mockContext();

        String result = statusCalculator.calculate(row, context);
        assertThat(result).isEqualTo("OPTIMAL");
    }

    // ========== PERFORMANCE TESTS ==========

    @Test
    void testCalculator_PerformanceWithManyCalculations() {
        Calculator<Double> heavyCalculator = (row, context) -> {
            Double base = row.getDouble("base", 1.0);

            // Simulate complex calculation
            double result = base;
            for (int i = 0; i < 1000; i++) {
                result = Math.sin(result) + Math.cos(result * 2);
            }

            return result;
        };

        Row row = mockRow(Map.of("base", 1.5));
        BaseContext<?> context = mockContext();

        // Should complete within reasonable time
        long startTime = System.currentTimeMillis();
        Double result = heavyCalculator.calculate(row, context);
        long endTime = System.currentTimeMillis();

        assertThat(result).isNotNull();
        assertThat(endTime - startTime).isLessThan(1000); // Should complete within 1 second
    }

    // ========== HELPER METHODS ==========

    private Row mockRow(Map<String, Object> data) {
        Row row = mock(Row.class);

        // Mock basic get method
        when(row.get(any(String.class))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return data.get(key);
        });

        // Set up default behavior for all two-parameter getter methods
        when(row.getString(any(String.class), any(String.class))).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            String defaultValue = inv.getArgument(1);
            Object value = data.get(key);
            return (value instanceof String && value != null) ? (String) value : defaultValue;
        });

        when(row.getInteger(any(String.class), any(Integer.class))).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            Integer defaultValue = inv.getArgument(1);
            Object value = data.get(key);
            return (value instanceof Integer && value != null) ? (Integer) value : defaultValue;
        });

        when(row.getLong(any(String.class), any(Long.class))).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            Long defaultValue = inv.getArgument(1);
            Object value = data.get(key);
            return (value instanceof Long && value != null) ? (Long) value : defaultValue;
        });

        when(row.getDouble(any(String.class), any(Double.class))).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            Double defaultValue = inv.getArgument(1);
            Object value = data.get(key);
            return (value instanceof Double && value != null) ? (Double) value : defaultValue;
        });

        when(row.getBoolean(any(String.class), any(Boolean.class))).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            Boolean defaultValue = inv.getArgument(1);
            Object value = data.get(key);
            return (value instanceof Boolean && value != null) ? (Boolean) value : defaultValue;
        });

        // Mock typed get methods for values that exist in data
        data.forEach((key, value) -> {
            when(row.get(eq(key), any(Class.class))).thenAnswer(invocation -> {
                Class<?> type = invocation.getArgument(1);
                Object val = data.get(key);
                return type.isInstance(val) ? val : null;
            });

            // Mock single-parameter specific type methods
            if (value instanceof String) {
                when(row.getString(key)).thenReturn((String) value);
            } else if (value instanceof Integer) {
                when(row.getInteger(key)).thenReturn((Integer) value);
            } else if (value instanceof Long) {
                when(row.getLong(key)).thenReturn((Long) value);
            } else if (value instanceof Double) {
                when(row.getDouble(key)).thenReturn((Double) value);
            } else if (value instanceof BigDecimal) {
                when(row.getBigDecimal(key)).thenReturn((BigDecimal) value);
            } else if (value instanceof LocalDate) {
                when(row.getLocalDate(key)).thenReturn((LocalDate) value);
            } else if (value instanceof LocalDateTime) {
                when(row.getLocalDateTime(key)).thenReturn((LocalDateTime) value);
            } else if (value instanceof Boolean) {
                when(row.getBoolean(key)).thenReturn((Boolean) value);
            }
        });

        return row;
    }

    private BaseContext<?> mockContext() {
        BaseContext<?> context = mock(BaseContext.class);

        Map<String, Object> params = new HashMap<>();
        Map<String, Object> metadata = new HashMap<>();

        when(context.getParam(any(String.class))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return params.get(key);
        });

        when(context.getParams()).thenReturn(params);
        when(context.getMetadata()).thenReturn(metadata);

        // Mock timing methods
        when(context.getExecutionTime()).thenReturn(0L);
        doAnswer(invocation -> {
            when(context.getStartTime()).thenReturn(System.currentTimeMillis());
            return null;
        }).when(context).startExecution();

        doAnswer(invocation -> {
            long endTime = System.currentTimeMillis();
            long startTime = context.getStartTime() != null ? context.getStartTime() : endTime;
            when(context.getEndTime()).thenReturn(endTime);
            when(context.getExecutionTime()).thenReturn(endTime - startTime);
            return null;
        }).when(context).endExecution();

        // Mock metadata addition
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object value = invocation.getArgument(1);
            metadata.put(key, value);
            return null;
        }).when(context).addMetadata(any(String.class), any());

        return context;
    }

    private BaseContext<?> mockContextWithParams(Map<String, Object> params) {
        BaseContext<?> context = mockContext();

        when(context.getParam(any(String.class))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return params.get(key);
        });

        when(context.getParams()).thenReturn(new HashMap<>(params));

        return context;
    }
}