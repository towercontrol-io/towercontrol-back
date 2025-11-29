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

public class MandatoryField implements CloneableObject<MandatoryField> {

    // Name to be given to the field
    private String name;

    // Type of value expected in the field, this will be stored as a String but the type is used
    // to make sure we have the correct format, and we let the front end manage this.
    // types are:
    // - string[,regex] (eg: url or url,^http[s]:// )
    // - number[,min,max]  (eg: number,0,100 or number,-50,50 or number,0 or number)
    // - decimal[,min,max] (eg: decimal,0.0,100.0 or decimal,-50.5,50.5 or decimal,0.0 or decimal)
    // - boolean
    // - date              (eg: date will be stored as EpocMs long value in a String)
    // - enum(val1|val2|val3)[,multiple] (eg: enum[red|green|blue] enum value are slugs, multiple means several values can be selected, stored as comma separated values)
    private String valueType;

    // slug to describe the purpose of this field, used for i18n
    private String description;

    // Short English description (non i18n) for quick understanding
    private String enDescription;

    public MandatoryField() {
    }

    public MandatoryField(String name, String valueType, String description, String enDescription) {
        this.name = name;
        this.valueType = valueType;
        this.description = description;
        this.enDescription = enDescription;
    }

    @Override
    public MandatoryField clone() {
        return new MandatoryField(this.name, this.valueType, this.description, this.enDescription);
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
}
