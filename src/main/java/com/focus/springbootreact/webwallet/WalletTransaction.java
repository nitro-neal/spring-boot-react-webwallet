package com.focus.springbootreact.webwallet;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class WalletTransaction {
    private  String transactionType;
    private String timestamp;
    private String amount;
    private String address;
    private String transactionId;
    private String debug;
}
