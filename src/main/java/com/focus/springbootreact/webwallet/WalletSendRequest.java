package com.focus.springbootreact.webwallet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WalletSendRequest {
    public String amount;
    public String address;

    @JsonCreator
    public WalletSendRequest(@JsonProperty("amount")String amount, @JsonProperty("address")String address) {
        this.amount = amount;
        this.address = address;
    }
}
