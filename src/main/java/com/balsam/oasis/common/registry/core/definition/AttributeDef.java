package com.balsam.oasis.common.registry.core.definition;

import java.util.function.Function;

import com.balsam.oasis.common.registry.processor.AttributeFormatter;
import com.balsam.oasis.common.registry.processor.Calculator;
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
    boolean transient_; // True for calculated/transient attributes

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
    Integer displayOrder; // Display order priority
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

    // LOV context metadata
    boolean lovValue; // Is this the value field for LOV?
    boolean lovLabel; // Is this the label field for LOV?

    // Private constructor - only accessible through builder
    private AttributeDef(BuilderStage<T> builder) {
        this.name = builder.name;
        this.aliasName = builder.aliasName;
        this.type = builder.type;
        this.filterable = builder.filterable;
        this.sortable = builder.sortable;
        this.primaryKey = builder.primaryKey;
        this.transient_ = builder.transient_;
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
        this.displayOrder = builder.displayOrder;
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
        // LOV context
        this.lovValue = builder.lovValue;
        this.lovLabel = builder.lovLabel;
    }

    /**
     * Static factory method to start building an attribute Returns TypeStage to
     * force type specification
     */
    public static TypeStage name(String name) {
        return new TypeStage(name);
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

    public boolean isTransient() {
        return transient_;
    }

    public boolean hasSortProperty() {
        return sortProperty != null && !sortProperty.trim().isEmpty();
    }

    /**
     * Stage 1: TypeStage - ONLY allows setting type This ensures type must be
     * specified immediately after attr()
     */
    public static class TypeStage {

        private final String name;

        private TypeStage(String name) {
            Preconditions.checkNotNull(name, "Attribute name cannot be null");
            Preconditions.checkArgument(!name.trim().isEmpty(), "Attribute name cannot be empty");
            this.name = name;
        }

        /**
         * Specify the type of this attribute This is the ONLY method available
         * at this stage
         *
         * @param type the Java class representing the attribute type
         * @return the builder stage with all configuration methods
         */
        public <T> BuilderStage<T> type(Class<T> type) {
            Preconditions.checkNotNull(type, "Type cannot be null");
            return new BuilderStage<>(name, type);
        }
    }

    /**
     * Stage 2: BuilderStage - all configuration methods available after type is
     * set This stage is generic and knows the attribute type
     */
    public static class BuilderStage<T> {

        private final String name;
        private final Class<T> type;
        private String aliasName;
        private boolean filterable = false;
        private boolean sortable = false;
        private boolean primaryKey = false;
        private boolean transient_ = false;
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
        private Integer displayOrder;
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

        // LOV context fields
        private boolean lovValue = false;
        private boolean lovLabel = false;

        private BuilderStage(String name, Class<T> type) {
            this.name = name;
            this.type = type;
            this.aliasName = name; // Default to same as name
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
        public BuilderStage<T> transient_(boolean transient_) {
            this.transient_ = transient_;
            if (transient_) {
                this.aliasName = null; // Transient attributes don't have DB columns
                this.sortable = false; // Cannot sort at DB level
                this.filterable = false; // Cannot filter at DB level
            }
            return this;
        }

        // Set calculator for transient attributes (renamed from calculator)
        public BuilderStage<T> calculated(Calculator<T> calculator) {
            this.calculator = calculator;
            this.transient_ = true; // Automatically mark as transient
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

        public BuilderStage<T> displayOrder(Integer displayOrder) {
            this.displayOrder = displayOrder;
            return this;
        }

        public BuilderStage<T> visible(boolean visible) {
            this.visible = visible;
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

        // LOV context builder methods
        public BuilderStage<T> lovValue(boolean lovValue) {
            this.lovValue = lovValue;
            return this;
        }

        public BuilderStage<T> lovLabel(boolean lovLabel) {
            this.lovLabel = lovLabel;
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
            if (transient_) {
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
