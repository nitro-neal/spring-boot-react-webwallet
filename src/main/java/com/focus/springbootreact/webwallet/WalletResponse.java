package com.focus.springbootreact.webwallet;

public class WalletResponse {

    private final String message;

    public WalletResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }
}
