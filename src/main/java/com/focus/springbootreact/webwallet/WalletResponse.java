package com.focus.springbootreact.webwallet;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WalletResponse {

    private String message;
    private String receiveAddress;
    private String balance;
}
