package com.focus.springbootreact.webwallet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
//import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


public class Fingerprint {
    public int fingerprint;

    @JsonCreator
    public Fingerprint(@JsonProperty("fingerprint")int fingerprint) {
        this.fingerprint = fingerprint;
    }

    public int getFingerprint()
    {
        return fingerprint;
    }

    public void setFingerprint(int fingerprint){
        this.fingerprint = fingerprint;
    }
}
