/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2026.
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

import java.util.ArrayList;
import java.util.List;

public class ProtocolId implements CloneableObject<ProtocolId> {

    // Define the described protocol ID type. There may be multiple protocol ID types for a given protocol.
    protected String name;

    // Description is a slug description of the protocol ID definition, so it can be i18n translated on front-end
    // this is more for sharing different information than the protocol hierarchy defined above.
    protected String description;

    // Human-readable English description (short) for non i18n usage
    protected String enDescription;

    // Mandatory fields
    protected List<MandatoryField> mandatoryFields;


    // --------------------------------

    @Override
    public ProtocolId clone() {
        ProtocolId p = new ProtocolId();
        p.setName(this.name);
        p.setDescription(this.description);
        p.setEnDescription(this.enDescription);
        p.setMandatoryFields(new ArrayList<>());
        if ( this.mandatoryFields != null ) {
            for ( MandatoryField mf : this.mandatoryFields ) {
                p.getMandatoryFields().add( mf.clone() );
            }
        }
        return p;
    }

    // --------------------------------


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public List<MandatoryField> getMandatoryFields() {
        return mandatoryFields;
    }

    public void setMandatoryFields(List<MandatoryField> mandatoryFields) {
        this.mandatoryFields = mandatoryFields;
    }
}
