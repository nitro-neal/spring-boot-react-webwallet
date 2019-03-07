package com.focus.springbootreact.webwallet;

import org.bitcoinj.core.*;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.core.listeners.TransactionConfidenceEventListener;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.KeyChainEventListener;
import org.bitcoinj.wallet.listeners.ScriptsChangeEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsSentEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

@RestController
public class APIController {

    @Autowired
    SimpMessagingTemplate websocket;

    Map<String, WalletAppKit> wallets = new HashMap<>();
    Map<String, WalletState> walletStates = new HashMap<>();

    NetworkParameters params = TestNet3Params.get();

    @RequestMapping(value = "/initwallet", method = RequestMethod.GET)
    public ResponseEntity<String> initwallet(@RequestHeader(value = "Fingerprint", required = true) String fingerprint) {
        System.out.println("initwallet called for fingerprint: " + fingerprint);

        if(wallets.get(fingerprint) != null) {
            sendWebWalletUpdate(fingerprint);
            return ResponseEntity.ok().build();
        }

        Runnable r = new Runnable() {
            public void run() {
                createWalletKit(fingerprint);
            }
        };

        new Thread(r).start();

        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/sendCoins", method = RequestMethod.POST)
    public ResponseEntity<String> sendCoins(@RequestHeader(value = "Fingerprint", required = true) String fingerprint,
                                            @RequestBody WalletSendRequest sendRequest) {
        System.out.println("sendCoins called for fingerprint: " + fingerprint + " with amount: " + sendRequest.amount + " and address: " + sendRequest.address);

        if(wallets.get(fingerprint) == null || sendRequest.amount == null || sendRequest.address == null) {
            return ResponseEntity.badRequest().build();
        }

        sendCoinsToAddress(fingerprint, sendRequest.amount, sendRequest.address);

        return ResponseEntity.ok().build();
    }

    private void sendCoinsToAddress(String fingerprint, String amount, String address) {

        WalletAppKit kit = wallets.get(fingerprint);
        final Coin amountToSend = Coin.parseCoin(amount);
        final Address addressToSend = Address.fromBase58(params, address);

        SendRequest req = SendRequest.to(addressToSend, amountToSend);
        req.feePerKb = Coin.parseCoin("0.00001");

        Wallet.SendResult sendResult = null;

        try {
            sendResult = kit.wallet().sendCoins(wallets.get(fingerprint).peerGroup(), req);
        } catch (InsufficientMoneyException e) {
            e.printStackTrace();
        }

        Transaction createdTx = sendResult.tx;
        System.out.println("createdTx: " + createdTx);

        final Wallet.SendResult finalSendResult = sendResult;
        sendResult.broadcastComplete.addListener(new Runnable() {
            @Override
            public void run() {
                // The wallet has changed now, it'll get auto saved shortly or when the app shuts down.
                walletStates.get(fingerprint).setBalance(wallets.get(fingerprint).wallet().getBalance().toFriendlyString());
                sendWebWalletUpdate(fingerprint);
                System.out.println("Sent coins onwards! Transaction hash is " + finalSendResult.tx.getHashAsString());
            }
        }, new Executor() {
            @Override
            public void execute(Runnable command) {

            }
        });
    }

    private void createWalletKit(String fingerprint) {
        WalletAppKit kit = new WalletAppKit(params, new File("."), "walletappkit-" + fingerprint) {
            @Override
            protected void onSetupCompleted() {
//                 This is called in a background thread after startAndWait is called
//                if (wallet().getKeyChainGroupSize() < 1)
//                    wallet().importKey(new ECKey());
                WalletState walletState = WalletState.builder()
                        .message("New Wallet")
                        .balance(wallet().getBalance().toFriendlyString())
                        .receiveAddress(wallet().freshReceiveAddress().toBase58())
                        .build();
                walletStates.put(fingerprint, walletState);
                sendWebWalletUpdate(fingerprint);
                System.out.println("Wallet Setup Complete. Waiting for blockchain download..");
            }
        };

        wallets.put(fingerprint, kit);

        //TODO: This doesn't work
        DownloadProgressTracker bListener = new DownloadProgressTracker() {
            @Override
            public void doneDownload() {
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!blockchain downloaded");
            }

            @Override
            public void progress(double pct, int blocksSoFar, Date date) {
                System.out.println("!!!!!!!!!!!Progress %: " + pct);
                //walletStats.get(fingerprint).downloadPercent = pct;

            }
        };

        kit.setDownloadListener(bListener);

        kit.startAsync();
        kit.awaitRunning();

        addListeners(kit, fingerprint);
    }

    private void addListeners(WalletAppKit kit, String fingerprint) {
        kit.wallet().addCoinsReceivedEventListener(new WalletCoinsReceivedEventListener() {
            @Override
            public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
                System.out.println("fingerprint: " + fingerprint + " -----> coins RECEIVED: tx" + tx);
                // TODO: Bug where if on the same block, newBalance is incorrect..
//                System.out.println("received: " + tx.getValue(wallet));
//                System.out.println("prev balance: " + prevBalance.toFriendlyString());
//                System.out.println("new balance: " + newBalance.toFriendlyString());
//                Coin amountSentToMe = tx.getValueSentToMe(kit.wallet());
//                Coin myNewBalance = amountSentToMe.plus(Coin.parseCoin(walletStates.get(fingerprint).getBalance()));

                walletStates.get(fingerprint).setBalance(newBalance.toFriendlyString());
                sendWebWalletUpdate(fingerprint);
            }
        });

        kit.wallet().addCoinsSentEventListener(new WalletCoinsSentEventListener() {
            @Override
            public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
                System.out.println("coins sent");
                walletStates.get(fingerprint).setBalance(newBalance.toFriendlyString());
                sendWebWalletUpdate(fingerprint);
            }
        });

        kit.wallet().addKeyChainEventListener(new KeyChainEventListener() {
            @Override
            public void onKeysAdded(List<ECKey> keys) {
                System.out.println("new key added");
            }
        });

        kit.wallet().addScriptsChangeEventListener(new ScriptsChangeEventListener() {
            @Override
            public void onScriptsChanged(Wallet wallet, List<Script> scripts, boolean isAddingScripts) {
                System.out.println("new script added");
            }
        });

        kit.wallet().addTransactionConfidenceEventListener(new TransactionConfidenceEventListener() {
            @Override
            public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx) {
//                System.out.println("-----> confidence changed: " + tx);
//                TransactionConfidence confidence = tx.getConfidence();
//                System.out.println("new block depth: " + confidence.getDepthInBlocks());
            }
        });

        System.out.println("send money to: " + kit.wallet().freshReceiveAddress().toString());
    }

    private void sendWebWalletUpdate(String fingerprint) {
        System.out.println("SENDING WEBSOCKET MESSAGE");
        websocket.convertAndSend(WebSocketConfiguration.MESSAGE_PREFIX + "/updateWallet-" + fingerprint, walletStates.get(fingerprint));
    }

    private void startKitDemo() {
        // blockchain dl tracker - https://github.com/bitcoinj/bitcoinj/blob/master/examples/src/main/java/org/bitcoinj/examples/RestoreFromSeed.java

        // First we configure the network we want to use.
        // The available options are:
        // - MainNetParams
        // - TestNet3Params
        // - RegTestParams
        // While developing your application you probably want to use the Regtest mode and run your local bitcoin network. Run bitcoind with the -regtest flag
        // To test you app with a real network you can use the testnet. The testnet is an alternative bitcoin network that follows the same rules as main network. Coins are worth nothing and you can get coins for example from http://faucet.xeno-genesis.com/
        //
        // For more information have a look at: https://bitcoinj.github.io/testing and https://bitcoin.org/en/developer-examples#testing-applications
        NetworkParameters params = TestNet3Params.get();

        // Now we initialize a new WalletAppKit. The kit handles all the boilerplate for us and is the easiest way to get everything up and running.
        // Have a look at the WalletAppKit documentation and its source to understand what's happening behind the scenes: https://github.com/bitcoinj/bitcoinj/blob/master/core/src/main/java/org/bitcoinj/kits/WalletAppKit.java
        WalletAppKit kit = new WalletAppKit(params, new File("."), "walletappkit-example");

        // In case you want to connect with your local bitcoind tell the kit to connect to localhost.
        // You must do that in reg test mode.
        //kit.connectToLocalHost();

        // Now we start the kit and sync the blockchain.
        // bitcoinj is working a lot with the Google Guava libraries. The WalletAppKit extends the AbstractIdleService. Have a look at the introduction to Guava services: https://github.com/google/guava/wiki/ServiceExplained

        System.out.println("before start async");
        kit.startAsync();
        kit.awaitRunning();
        System.out.println("after start async");

        kit.wallet().addCoinsReceivedEventListener(new WalletCoinsReceivedEventListener() {
            @Override
            public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
                System.out.println("-----> coins resceived: " + tx);
                System.out.println("received: " + tx.getValue(wallet));
            }
        });

        kit.wallet().addCoinsSentEventListener(new WalletCoinsSentEventListener() {
            @Override
            public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
                System.out.println("coins sent");
            }
        });

        kit.wallet().addKeyChainEventListener(new KeyChainEventListener() {
            @Override
            public void onKeysAdded(List<ECKey> keys) {
                System.out.println("new key added");
            }
        });

        kit.wallet().addScriptsChangeEventListener(new ScriptsChangeEventListener() {
            @Override
            public void onScriptsChanged(Wallet wallet, List<Script> scripts, boolean isAddingScripts) {
                System.out.println("new script added");
            }
        });

        kit.wallet().addTransactionConfidenceEventListener(new TransactionConfidenceEventListener() {
            @Override
            public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx) {
                System.out.println("-----> confidence changed: " + tx);
                TransactionConfidence confidence = tx.getConfidence();
                System.out.println("new block depth: " + confidence.getDepthInBlocks());
            }
        });

        // Ready to run. The kit syncs the blockchain and our wallet event listener gets notified when something happens.
        // To test everything we create and print a fresh receiving address. Send some coins to that address and see if everything works.
        System.out.println("send money to: " + kit.wallet().freshReceiveAddress().toString());

        // Make sure to properly shut down all the running services when you manually want to stop the kit. The WalletAppKit registers a runtime ShutdownHook so we actually do not need to worry about that when our application is stopping.
        //System.out.println("shutting down again");
        //kit.stopAsync();
        //kit.awaitTerminated();
    }

}
