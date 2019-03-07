package com.focus.springbootreact.webwallet;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
public class WalletState {

    private String message;
    private String receiveAddress;
    private String balance;
    private List<WalletTransaction> transactions;
}
