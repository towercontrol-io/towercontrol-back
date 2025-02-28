package com.disk91.common.pdb.entities;


import com.disk91.common.tools.CloneableObject;
import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Transient;

@Entity
@Table(
        name = "common_params",
        indexes = {
                @Index(name = "idx_com_params_key", columnList = "param_key", unique = true)
        }
)
public class Param implements CloneableObject<Param> {

    @Transient
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    @Id
    @Column(name = "param_key", nullable = false, unique = true)
    protected String paramKey;

    @Column(name = "string_value", nullable = true)
    protected String stringValue;

    @Column(name = "long_value", nullable = true)
    protected long longValue;

    @Column(name = "double_value", nullable = true)
    protected double doubleValue;

    // ================================================================================================================
    // Clonable implementation

    public Param clone() {
        Param clone = new Param();
        clone.paramKey = this.paramKey;
        clone.stringValue = this.stringValue;
        clone.longValue = this.longValue;
        clone.doubleValue = this.doubleValue;
        return clone;
    }

    // ================================================================================================================
    // Getters & Setters


    public String getParamKey() {
        return paramKey;
    }

    public void setParamKey(String paramKey) {
        this.paramKey = paramKey;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public long getLongValue() {
        return longValue;
    }

    public void setLongValue(long longValue) {
        this.longValue = longValue;
    }

    public double getDoubleValue() {
        return doubleValue;
    }

    public void setDoubleValue(double doubleValue) {
        this.doubleValue = doubleValue;
    }
}
