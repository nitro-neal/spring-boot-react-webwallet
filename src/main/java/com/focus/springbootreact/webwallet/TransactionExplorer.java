package com.focus.springbootreact.webwallet;

import org.bitcoinj.core.*;
import org.bitcoinj.wallet.Wallet;

public class TransactionExplorer {

    public String getSentToAddressFromOutput(Transaction tx, Wallet wallet) {
        for(TransactionOutput output : tx.getOutputs()) {
            if(output.isMine(wallet)) {
                return output.getScriptPubKey().getToAddress(APIController.PARAMS).toString();
            }
        }
        return "";
    }

    public String getFromAddress(Transaction tx, Wallet wallet) {
        for(TransactionInput input : tx.getInputs()) {
            if(input.getConnectedOutput() != null && input.getConnectedOutput().getAddressFromP2PKHScript(APIController.PARAMS) != null) {
                return input.getConnectedOutput().getAddressFromP2PKHScript(APIController.PARAMS).toBase58();
            }
        }
            return "";
    }

    public String getSentToAddress(Transaction tx, Wallet wallet, Coin amountSent) {
        for(TransactionOutput output : tx.getOutputs()) {
            if(!output.isMine(wallet) && amountSent.equals(output.getValue()) ){
                return output.getAddressFromP2PKHScript(APIController.PARAMS).toBase58();
            }
        }
        return "";
    }

}
