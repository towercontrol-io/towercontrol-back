/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2024.
 *
 *    Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 *    and associated documentation files (the "Software"), to deal in the Software without restriction,
 *    including without limitation the rights to use, copy, modify, merge, publish, distribute,
 *    sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 *    furnished to do so, subject to the following conditions:
 *
 *    The above copyright notice and this permission notice shall be included in all copies or
 *    substantial portions of the Software.
 *
 *    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *    FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 *    OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *    WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 *    IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.disk91.capture.mdb.entities.sub;

import com.disk91.common.tools.CloneableObject;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class MandatoryField implements CloneableObject<MandatoryField> {

    // Name to be given to the field
    private String name;

    // When true a control can be made, this is not mandatory
    private boolean unique;

    // When true, the value is stored encrypted in database
    private boolean encrypted;

    // Type of value expected in the field, this will be stored as a String but the type is used
    // to make sure we have the correct format, and we let the front end manage this.
    // types are:
    // - string[,regex] (eg: url or url,^http[s]:// )
    // - number[,min,max]  (eg: number,0,100 or number,-50,50 or number,0 or number)
    // - decimal[,min,max] (eg: decimal,0.0,100.0 or decimal,-50.5,50.5 or decimal,0.0 or decimal)
    // - boolean
    // - date              (eg: date will be stored as EpocMs long value in a String)
    // - enum[val1|val2|val3](,multiple) (eg: enum[red|green|blue] enum value are slugs, multiple means several values can be selected, stored as | separated values)
    private String valueType;

    // slug to describe the purpose of this field, used for i18n
    private String description;

    // Short English description (non i18n) for quick understanding
    private String enDescription;

    public MandatoryField() {
    }

    public MandatoryField(String name, String valueType, String description, String enDescription) {
        this.name = name;
        this.unique = false;
        this.encrypted = false;
        this.valueType = valueType;
        this.description = description;
        this.enDescription = enDescription;
    }

    public MandatoryField(String name,boolean unique, boolean encrypted, String valueType, String description, String enDescription) {
        this.name = name;
        this.unique = unique;
        this.encrypted = encrypted;
        this.valueType = valueType;
        this.description = description;
        this.enDescription = enDescription;
    }

    @Override
    public MandatoryField clone() {
        return new MandatoryField(this.name, this.unique, this.encrypted, this.valueType, this.description, this.enDescription);
    }

    /**
     * Validates whether the given value conforms to the syntax defined in valueType.
     * Supports: string[,regex], number[,min,max], decimal[,min,max], boolean, date, enum[val1|val2|val3][,multiple]
     * @param value - The string value to validate
     * @return true if the value is valid according to the valueType definition, false otherwise
     */
    public boolean isValueValid(String value) {
        if (valueType == null || value == null) return false;

        // --- string[,regex] ---
        if (valueType.startsWith("string")) {
            int commaIdx = valueType.indexOf(',');
            if (commaIdx == -1) {
                // Plain string, any non-null value is valid
                return true;
            }
            // Validate against the provided regex
            String regex = valueType.substring(commaIdx + 1);
            try {
                return Pattern.compile(regex).matcher(value).find();
            } catch (PatternSyntaxException e) {
                return false;
            }
        }

        // --- boolean ---
        if (valueType.equals("boolean")) {
            return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
        }

        // --- date (stored as EpochMs long in a String) ---
        if (valueType.equals("date")) {
            try {
                long epoch = Long.parseLong(value);
                return epoch >= 0;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        // --- number[,min,max] ---
        if (valueType.startsWith("number")) {
            long numValue;
            try {
                numValue = Long.parseLong(value);
            } catch (NumberFormatException e) {
                return false;
            }
            // Parse optional min/max bounds
            String[] parts = valueType.split(",", -1);
            if (parts.length >= 2 && !parts[1].isEmpty()) {
                try {
                    long min = Long.parseLong(parts[1]);
                    if (numValue < min) return false;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            if (parts.length >= 3 && !parts[2].isEmpty()) {
                try {
                    long max = Long.parseLong(parts[2]);
                    if (numValue > max) return false;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return true;
        }

        // --- decimal[,min,max] ---
        if (valueType.startsWith("decimal")) {
            double decValue;
            try {
                decValue = Double.parseDouble(value);
            } catch (NumberFormatException e) {
                return false;
            }
            // Parse optional min/max bounds
            String[] parts = valueType.split(",", -1);
            if (parts.length >= 2 && !parts[1].isEmpty()) {
                try {
                    double min = Double.parseDouble(parts[1]);
                    if (decValue < min) return false;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            if (parts.length >= 3 && !parts[2].isEmpty()) {
                try {
                    double max = Double.parseDouble(parts[2]);
                    if (decValue > max) return false;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return true;
        }

        // --- enum[val1|val2|val3][,multiple] ---
        if (valueType.startsWith("enum[")) {
            int closeBracket = valueType.indexOf(']');
            if (closeBracket == -1) return false;

            // Extract allowed values from enum[...]
            String enumContent = valueType.substring(5, closeBracket);
            String[] allowedValues = enumContent.split("\\|");

            // Check whether multiple selection is allowed
            boolean multiple = valueType.substring(closeBracket + 1).contains("multiple");

            if (multiple) {
                // Each | separated token must be a valid enum value
                String[] selectedValues = value.split("\\|");
                return Arrays.stream(selectedValues)
                        .map(String::trim)
                        .allMatch(v -> Arrays.asList(allowedValues).contains(v));
            } else {
                // Single value must exactly match one of the allowed values
                return Arrays.asList(allowedValues).contains(value.trim());
            }
        }

        return false;
    }

    // === GETTER / SETTER ===

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEnDescription() {
        return enDescription;
    }

    public void setEnDescription(String enDescription) {
        this.enDescription = enDescription;
    }

    public boolean isUnique() {
        return unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }
}
