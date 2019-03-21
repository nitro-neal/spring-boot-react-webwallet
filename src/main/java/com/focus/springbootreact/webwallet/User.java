package com.focus.springbootreact.webwallet;

public class User {

    public String fingerprint;
    public String currentCoinAmount;
    public String receivedCount;
    public String sentCount;

    public User(String fingerprint, String currentCoinAmount, String receivedCount, String sentCount) {
        this.fingerprint = fingerprint;
        this.currentCoinAmount = currentCoinAmount;
        this.receivedCount = receivedCount;
        this.sentCount = sentCount;
    }
}
