package com.disk91.common.tools;

public class ClonableString implements CloneableObject<ClonableString> {

    protected String value;

    public ClonableString() {
        this.value = null;
    }

    public ClonableString(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public ClonableString clone() {
        return new ClonableString(this.value);
    }

}
