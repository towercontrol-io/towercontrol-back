package com.disk91.common.interfaces.chirpstack.sub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)

public class ChirpstackMetadata {
    /*
                 "metadata":{
                    network": "helium_iot",
                    "gateway_long": "3.0",
                    "regi": "EU868",
                    "gateway_name": "myt...ch",
                    "gateway_id": "11o8....f13XA",
                    "gateway_lat": "45.8",
                    "gateway_h3index": "8c1..."
                },
            }
     */

    private String network;
    private String gateway_id;
    private String gateway_name;
    private String regi;
    private String gateway_lat;
    private String gateway_long;
    private String gateway_h3index;


    // ---


    public String getGateway_lat() {
        return gateway_lat;
    }

    public void setGateway_lat(String gateway_lat) {
        this.gateway_lat = gateway_lat;
    }

    public String getGateway_long() {
        return gateway_long;
    }

    public void setGateway_long(String gateway_long) {
        this.gateway_long = gateway_long;
    }

    public String getGateway_h3index() {
        return gateway_h3index;
    }

    public void setGateway_h3index(String gateway_h3index) {
        this.gateway_h3index = gateway_h3index;
    }

    public String getGateway_id() {
        return gateway_id;
    }

    public void setGateway_id(String gateway_id) {
        this.gateway_id = gateway_id;
    }

    public String getGateway_name() {
        return gateway_name;
    }

    public void setGateway_name(String gateway_name) {
        this.gateway_name = gateway_name;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String getRegi() {
        return regi;
    }

    public void setRegi(String regi) {
        this.regi = regi;
    }
}
