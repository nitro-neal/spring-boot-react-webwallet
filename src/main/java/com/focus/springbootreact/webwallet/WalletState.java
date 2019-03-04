package com.focus.springbootreact.webwallet;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class WalletState {

    private String message;
    private String receiveAddress;
    private String balance;
}
