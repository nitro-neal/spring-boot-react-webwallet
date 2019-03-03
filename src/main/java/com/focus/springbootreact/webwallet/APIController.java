package com.focus.springbootreact.webwallet;

import org.bitcoinj.core.*;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.core.listeners.TransactionConfidenceEventListener;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.KeyChainEventListener;
import org.bitcoinj.wallet.listeners.ScriptsChangeEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsSentEventListener;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
public class APIController {

    Map<String, WalletAppKit> wallets = new HashMap<>();
    Map<String, WalletStat> walletStats = new HashMap<>();

    @RequestMapping(value = "/initwallet", method = RequestMethod.GET)
    public WalletResponse initwallet(@RequestHeader(value = "Fingerprint", required = true) String fingerprint) {
        System.out.println("initwallet called for fingerprint: " + fingerprint);

        if(wallets.get(fingerprint) != null) {
            return WalletResponse.builder().message("Wallet already exists and is in ready state").build();
        }

        Runnable r = new Runnable() {
            public void run() {
                createWalletKit(fingerprint);
            }
        };

        new Thread(r).start();

        return WalletResponse.builder().message("Wallet creation started").build();
    }

    //TODO: Implement sockets and fix DownloadProgressTracker
    @RequestMapping(value = "/getWalletDownloadProgress", method = RequestMethod.GET)
    public WalletResponse getWalletDownloadProgress(@RequestHeader(value = "Fingerprint", required = true) String fingerprint) {
        System.out.println("getWalletDownloadProgress called for fingerprint: " + fingerprint);

        if(wallets.get(fingerprint) == null) {
            throw new RuntimeException("Wallet does not exist!");
        }

        return WalletResponse.builder().message("downloadpercent: " + walletStats.get(fingerprint).downloadPercent).build();
    }

    @RequestMapping(value = "/getReceiveAddress", method = RequestMethod.GET)
    public WalletResponse getReceiveAddress(@RequestHeader(value = "Fingerprint", required = true) String fingerprint) {
        System.out.println("getReceiveAddress called for fingerprint: " + fingerprint);

        if(wallets.get(fingerprint) == null) {
            throw new RuntimeException("Wallet does not exist!");
        }

        return WalletResponse.builder().receiveAddress(wallets.get(fingerprint).wallet().freshReceiveAddress().toString()).build();
    }

    @RequestMapping(value = "/getBalance", method = RequestMethod.GET)
    public WalletResponse getBalance(@RequestHeader(value = "Fingerprint", required = true) String fingerprint) {
        System.out.println("getBalance called for fingerprint: " + fingerprint);

        if(wallets.get(fingerprint) == null) {
            throw new RuntimeException("Wallet does not exist!");
        }

        return WalletResponse.builder().balance(wallets.get(fingerprint).wallet().getBalance().toFriendlyString()).build();
    }

    private void createWalletKit(String fingerprint) {
        NetworkParameters params = TestNet3Params.get();
        WalletAppKit kit = new WalletAppKit(params, new File("."), "walletappkit-" + fingerprint);
        wallets.put(fingerprint, kit);
        walletStats.put(fingerprint, new WalletStat());

        DownloadProgressTracker bListener = new DownloadProgressTracker() {
            @Override
            public void doneDownload() {
                System.out.println("blockchain downloaded");
            }

            @Override
            public void progress(double pct, int blocksSoFar, Date date) {
                System.out.println("!!!!!!!!!!!Progress %: " + pct);
                walletStats.get(fingerprint).downloadPercent = pct;

            }
        };

        kit.setDownloadListener(bListener);

        kit.startAsync();
        kit.awaitRunning();

        addListeners(kit);

    }

    private void addListeners(WalletAppKit kit) {
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

        System.out.println("send money to: " + kit.wallet().freshReceiveAddress().toString());
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
