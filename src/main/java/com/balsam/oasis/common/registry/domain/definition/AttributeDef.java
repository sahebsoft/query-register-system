package com.balsam.oasis.common.registry.domain.definition;

import java.util.function.Function;

import com.balsam.oasis.common.registry.domain.processor.AttributeFormatter;
import com.balsam.oasis.common.registry.domain.processor.Calculator;
import com.google.common.base.Preconditions;

import lombok.Value;

/**
 * Immutable attribute definition for both regular and transient attributes.
 * Uses staged builder pattern to enforce type specification at compile time.
 * 
 * <p>
 * Attributes represent fields in the query result and can be:
 * </p>
 * <ul>
 * <li><b>Regular attributes</b>: Mapped from database columns via
 * aliasName</li>
 * <li><b>Transient attributes</b>: Calculated dynamically using calculator
 * function</li>
 * </ul>
 *
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * // Regular attribute from database
 * AttributeDef&lt;String&gt; name = AttributeDef.&lt;String&gt;builder()
 *         .name("userName")
 *         .type(String.class)
 *         .aliasName("full_name")
 *         .filterable(true)
 *         .sortable(true)
 *         .build();
 *
 * // Transient calculated attribute
 * AttributeDef&lt;BigDecimal&gt; totalValue = AttributeDef.&lt;BigDecimal&gt;builder()
 *         .name("totalValue")
 *         .type(BigDecimal.class)
 *         .transient_(true)
 *         .calculator((row, context) -> calculateTotal(row))
 *         .build();
 * </pre>
 *
 * @param <T> the type of the attribute value
 * @author Query Registration System
 * @since 1.0
 */
@Value
public class AttributeDef<T> {

    String name; // Frontend/API name
    Class<T> type; // Java type for automatic conversion
    String aliasName; // Database column name (null for transient attributes)
    boolean filterable;
    boolean sortable;
    boolean primaryKey;
    boolean virtual; // True for calculated/transient attributes
    boolean selected; // False to exclude from response/metadata unless explicitly requested

    // For regular attributes: formats the value to string
    AttributeFormatter<T> formatter;

    // For transient attributes: calculates the value from row data
    Calculator<T> calculator;

    // For transient attributes: which regular attribute to use for sorting
    String sortProperty;

    // Security rule determines if user can see this attribute
    Function<Object, Boolean> securityRule;

    String description;

    // UI metadata fields for frontend display
    String label; // Display label for the attribute
    String labelKey; // i18n key for the label
    String width; // Display width (e.g., "100px", "20%")
    String flex; // Flex value for flexible layouts (e.g., "1", "2")

    // Table/Grid context metadata
    String headerText; // Column header for tables (alternative to label)
    String alignment; // Column alignment: left, center, right
    String headerStyle; // CSS style for table header
    boolean visible; // Default visibility in table

    // Form context metadata
    String placeholder; // Input placeholder text
    String helpText; // Help text for form fields
    String inputType; // Input type: text, number, date, select, etc.
    boolean required; // Required in forms
    Integer maxLength; // Maximum input length
    Integer minLength; // Minimum input length
    String pattern; // Validation regex pattern
    String validationMsg; // Validation error message

    // Private constructor - only accessible through builder
    private AttributeDef(BuilderStage<T> builder) {
        this.name = builder.name;
        this.aliasName = builder.aliasName;
        this.type = builder.type;
        this.filterable = builder.filterable;
        this.sortable = builder.sortable;
        this.primaryKey = builder.primaryKey;
        this.virtual = builder.virtual;
        this.selected = builder.selected;
        this.formatter = builder.formatter;
        this.calculator = builder.calculator;
        this.sortProperty = builder.sortProperty;
        this.securityRule = builder.securityRule;
        this.description = builder.description;
        this.label = builder.label;
        this.labelKey = builder.labelKey;
        this.width = builder.width;
        this.flex = builder.flex;
        // Table context
        this.headerText = builder.headerText;
        this.alignment = builder.alignment;
        this.headerStyle = builder.headerStyle;
        this.visible = builder.visible;
        // Form context
        this.placeholder = builder.placeholder;
        this.helpText = builder.helpText;
        this.inputType = builder.inputType;
        this.required = builder.required;
        this.maxLength = builder.maxLength;
        this.minLength = builder.minLength;
        this.pattern = builder.pattern;
        this.validationMsg = builder.validationMsg;
    }

    /**
     * Static factory method to start building an attribute
     * Type defaults to Object.class if not specified
     */
    public static BuilderStage<Object> name(String name) {
        Preconditions.checkNotNull(name, "Attribute name cannot be null");
        Preconditions.checkArgument(!name.trim().isEmpty(), "Attribute name cannot be empty");
        return new BuilderStage<>(name, Object.class);
    }

    public boolean isSecured() {
        return securityRule != null;
    }

    public boolean hasFormatter() {
        return formatter != null;
    }

    public boolean hasCalculator() {
        return calculator != null;
    }

    public boolean isVirual() {
        return virtual;
    }

    public boolean hasSortProperty() {
        return sortProperty != null && !sortProperty.trim().isEmpty();
    }

    /**
     * BuilderStage - all configuration methods available
     * This stage is generic and knows the attribute type
     */
    public static class BuilderStage<T> {

        private final String name;
        private final Class<T> type;
        private String aliasName;
        private boolean filterable = true; // Default true for non-virtual attributes
        private boolean sortable = true; // Default true for non-virtual attributes
        private boolean primaryKey = false;
        private boolean virtual = false;
        private boolean selected = true; // Default true - included in response/metadata
        private AttributeFormatter<T> formatter;
        private Calculator<T> calculator;
        private String sortProperty;
        private Function<Object, Boolean> securityRule;
        private String description;

        // UI metadata fields
        private String label;
        private String labelKey;
        private String width;
        private String flex;

        // Table context fields
        private String headerText;
        private String alignment = "left";
        private String headerStyle; // CSS style for header
        private boolean visible = true;

        // Form context fields
        private String placeholder;
        private String helpText;
        private String inputType = "text";
        private boolean required = false;
        private Integer maxLength;
        private Integer minLength;
        private String pattern;
        private String validationMsg;

        @SuppressWarnings("unchecked")
        private BuilderStage(String name, Class<T> type) {
            this.name = name;
            this.type = type != null ? type : (Class<T>) Object.class;
            this.aliasName = name; // Default to same as name
            // filterable and sortable default to true for regular attributes
            this.filterable = true;
            this.sortable = true;
        }

        /**
         * Specify or change the type of this attribute
         * Creates a new builder with the specified type and copies all settings
         *
         * @param newType the Java class representing the attribute type
         * @return a new builder stage with the specified type
         */
        @SuppressWarnings("unchecked")
        public <U> BuilderStage<U> type(Class<U> newType) {
            Preconditions.checkNotNull(newType, "Type cannot be null");
            BuilderStage<U> newBuilder = new BuilderStage<>(this.name, newType);
            // Copy all current settings
            newBuilder.aliasName = this.aliasName;
            newBuilder.filterable = this.filterable;
            newBuilder.sortable = this.sortable;
            newBuilder.primaryKey = this.primaryKey;
            newBuilder.virtual = this.virtual;
            newBuilder.selected = this.selected;
            newBuilder.formatter = (AttributeFormatter<U>) this.formatter;
            newBuilder.calculator = (Calculator<U>) this.calculator;
            newBuilder.sortProperty = this.sortProperty;
            newBuilder.securityRule = this.securityRule;
            newBuilder.description = this.description;
            // UI metadata
            newBuilder.label = this.label;
            newBuilder.labelKey = this.labelKey;
            newBuilder.width = this.width;
            newBuilder.flex = this.flex;
            // Table context
            newBuilder.headerText = this.headerText;
            newBuilder.alignment = this.alignment;
            newBuilder.headerStyle = this.headerStyle;
            newBuilder.visible = this.visible;
            // Form context
            newBuilder.placeholder = this.placeholder;
            newBuilder.helpText = this.helpText;
            newBuilder.inputType = this.inputType;
            newBuilder.required = this.required;
            newBuilder.maxLength = this.maxLength;
            newBuilder.minLength = this.minLength;
            newBuilder.pattern = this.pattern;
            newBuilder.validationMsg = this.validationMsg;
            return newBuilder;
        }

        // sql column/alias name
        public BuilderStage<T> aliasName(String column) {
            this.aliasName = column;
            return this;
        }

        public BuilderStage<T> filterable(boolean filterable) {
            this.filterable = filterable;
            return this;
        }

        public BuilderStage<T> sortable(boolean sortable) {
            this.sortable = sortable;
            return this;
        }

        public BuilderStage<T> primaryKey(boolean primaryKey) {
            this.primaryKey = primaryKey;
            return this;
        }

        public BuilderStage<T> formatter(AttributeFormatter<T> formatter) {
            this.formatter = formatter;
            return this;
        }

        // Mark as transient and provide calculator
        public BuilderStage<T> virtual(boolean virtual) {
            this.virtual = virtual;
            if (virtual) {
                this.aliasName = null; // Transient attributes don't have DB columns
                this.sortable = false; // Cannot sort at DB level
                this.filterable = false; // Cannot filter at DB level
            }
            return this;
        }

        // Set calculator for transient attributes (renamed from calculator)
        public BuilderStage<T> calculated(Calculator<T> calculator) {
            this.calculator = calculator;
            this.virtual = true; // Automatically mark as transient
            this.sortable = false; // Cannot sort at DB level
            this.filterable = false; // Cannot filter at DB level
            this.aliasName = null;
            // Don't automatically disable sortable/filterable - will validate later
            return this;
        }

        // Specify which regular attribute to use for sorting this transient attribute
        public BuilderStage<T> sortProperty(String attributeName) {
            this.sortProperty = attributeName;
            return this;
        }

        public BuilderStage<T> secure(Function<Object, Boolean> rule) {
            this.securityRule = rule;
            return this;
        }

        public BuilderStage<T> description(String description) {
            this.description = description;
            return this;
        }

        // UI metadata builder methods
        public BuilderStage<T> label(String label) {
            this.label = label;
            return this;
        }

        public BuilderStage<T> labelKey(String labelKey) {
            this.labelKey = labelKey;
            return this;
        }

        public BuilderStage<T> width(String width) {
            this.width = width;
            return this;
        }

        public BuilderStage<T> flex(String flex) {
            this.flex = flex;
            return this;
        }

        // Table context builder methods
        public BuilderStage<T> headerText(String headerText) {
            this.headerText = headerText;
            return this;
        }

        public BuilderStage<T> alignment(String alignment) {
            this.alignment = alignment;
            return this;
        }

        public BuilderStage<T> headerStyle(String headerStyle) {
            this.headerStyle = headerStyle;
            return this;
        }

        public BuilderStage<T> visible(boolean visible) {
            this.visible = visible;
            return this;
        }

        public BuilderStage<T> selected(boolean selected) {
            this.selected = selected;
            return this;
        }

        // Form context builder methods
        public BuilderStage<T> placeholder(String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        public BuilderStage<T> helpText(String helpText) {
            this.helpText = helpText;
            return this;
        }

        public BuilderStage<T> inputType(String inputType) {
            this.inputType = inputType;
            return this;
        }

        public BuilderStage<T> required(boolean required) {
            this.required = required;
            return this;
        }

        public BuilderStage<T> maxLength(Integer maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        public BuilderStage<T> minLength(Integer minLength) {
            this.minLength = minLength;
            return this;
        }

        public BuilderStage<T> pattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        public BuilderStage<T> validationMsg(String validationMsg) {
            this.validationMsg = validationMsg;
            return this;
        }

        /**
         * Build the immutable AttributeDef instance
         */
        public AttributeDef<T> build() {
            validate();
            return new AttributeDef<>(this);
        }

        private void validate() {
            if (virtual) {
                // Transient attributes must have a calculator
                if (calculator == null) {
                    throw new IllegalStateException("Transient attribute '" + name + "' must have a calculator");
                }
                // Transient attributes should not have DB column
                aliasName = null;
                // Transient attributes cannot be primary keys
                if (primaryKey) {
                    throw new IllegalStateException("Transient attributes cannot be primary keys");
                }
                // Transient attributes can only be sortable if they have a sortProperty
                if (sortable && (sortProperty == null || sortProperty.trim().isEmpty())) {
                    throw new IllegalStateException(
                            "Transient attribute '" + name + "' can only be sortable if it has a sortProperty");
                }
                // Transient attributes cannot be directly filterable (filtering happens at DB
                // level)
                if (filterable) {
                    throw new IllegalStateException("Transient attribute '" + name
                            + "' cannot be filterable (filtering happens at DB level, before calculation)");
                }
            } else {
                // Regular attributes: ensure aliasName is set (defaults to name if not
                // specified)
                if (aliasName == null || aliasName.trim().isEmpty()) {
                    aliasName = name;
                }
                // Regular attributes should not have calculator
                if (calculator != null) {
                    throw new IllegalStateException("Regular attribute '" + name
                            + "' should not have a calculator. Use calculated() for transient attributes.");
                }
                // Regular attributes should not have sortProperty (they sort by themselves)
                if (sortProperty != null && !sortProperty.trim().isEmpty()) {
                    throw new IllegalStateException("Regular attribute '" + name
                            + "' should not have a sortProperty. This is only for transient attributes.");
                }
            }
        }
    }

}
