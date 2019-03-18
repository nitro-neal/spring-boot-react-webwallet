package com.focus.springbootreact.webwallet;

import org.bitcoinj.core.*;
import org.bitcoinj.core.listeners.TransactionConfidenceEventListener;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.SPVBlockStore;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.concurrent.Executor;


@RestController
public class APIController {

    @Autowired
    SimpMessagingTemplate websocket;

    private TransactionExplorer transactionExplorer = new TransactionExplorer();

    private static final File BLOCKCHAIN_FILE = new File("runonboot-block.dat");
    public static final NetworkParameters PARAMS = TestNet3Params.get();
    private static PeerGroup SHARED_PEER_GROUP;

    private static List<Wallet> AVAILABLE_WALLETS = new ArrayList<>();
    private static Set<String> FINGERPRINTS = new HashSet<>();
    private static Map<String, Wallet> WALLETS = new HashMap<>();
    private static Map<String, WalletState> WALLET_STATES = new HashMap<>();

    private static int WALLET_COUNTER = 0;
    private static int WALLET_COUNT = 20;
    private static boolean LOAD_FROM_FILE = false;

    public static void RunOnBoot() throws Exception {

        System.out.println("---------------------------- RUN ON BOOT STARTING...");

        for (int i = 0; i < WALLET_COUNT; i++) {
            Wallet w = null;
            File file = new File("wallet-" + i);
            if(LOAD_FROM_FILE && file.exists()) {
                System.out.println("Loading wallet from file.. ");
                w = Wallet.loadFromFile(file);
            } else {
                System.out.println("No wallet file exists, creating new wallet.. ");
                w = new Wallet(PARAMS);
            }
            w.allowSpendingUnconfirmedTransactions();
            AVAILABLE_WALLETS.add(w);
        }

        BlockStore blockStore = new SPVBlockStore(PARAMS, BLOCKCHAIN_FILE);
        BlockChain blockChain = new BlockChain(PARAMS, AVAILABLE_WALLETS, blockStore);
        SHARED_PEER_GROUP = new PeerGroup(PARAMS, blockChain);
        SHARED_PEER_GROUP.addPeerDiscovery(new DnsDiscovery(PARAMS));

        for (Wallet w : AVAILABLE_WALLETS) {
            SHARED_PEER_GROUP.addWallet(w);
        }

        //Starting peerGroup;
        SHARED_PEER_GROUP.startAsync();

        //Start download blockchain
        SHARED_PEER_GROUP.downloadBlockChain();

        for (int i = 0; i < WALLET_COUNT; i++) {
            AVAILABLE_WALLETS.get(i).saveToFile(new File("wallet-" + i));
        }

        System.out.println("---------------------------- RUN ON BOOT FINISHED AND READY!");
    }

    @RequestMapping(value = "/initwallet", method = RequestMethod.GET)
    public ResponseEntity<String> initwallet(@RequestHeader(value = "Fingerprint", required = true) String fingerprint) {
        System.out.println("initwallet called for fingerprint: " + fingerprint);

        if(FINGERPRINTS.contains(fingerprint)) {

            //WALLETS.get(fingerprint).cleanup();
            sendWebWalletUpdate(fingerprint);
            return ResponseEntity.ok().build();
        }

        FINGERPRINTS.add(fingerprint);
        setupWalletForFingeprint(fingerprint);

        return ResponseEntity.ok().build();
    }

    private void setupWalletForFingeprint(String fingerprint) {
        WALLETS.put(fingerprint, AVAILABLE_WALLETS.get(WALLET_COUNTER));
        WALLET_COUNTER ++;

        if(WALLET_COUNTER >= WALLET_COUNT) {
            WALLET_COUNTER = 0;
        }

        Wallet w = WALLETS.get(fingerprint);
        System.out.println("Wallet about to be used: " + w);

        addListeners(w, fingerprint);

        WalletState walletState = WalletState.builder()
                .message("New Wallet")
                .balance(getFormattedBalance(w.getBalance()))
                .receiveAddress(w.freshReceiveAddress().toBase58())
                .transactions(new ArrayList<>())
                .build();

        WALLET_STATES.put(fingerprint, walletState);

        System.out.println(" --------------------- WALLET STATES READY! ---------------- FOR : " + fingerprint);

        sendWebWalletUpdate(fingerprint);
    }

    @RequestMapping(value = "/sendCoins", method = RequestMethod.POST)
    public ResponseEntity<String> sendCoins(@RequestHeader(value = "Fingerprint", required = true) String fingerprint,
                                            @RequestBody WalletSendRequest sendRequest) {
        System.out.println("sendCoins called for fingerprint: " + fingerprint + " with amount: " + sendRequest.amount + " and address: " + sendRequest.address);

        if(WALLETS.get(fingerprint) == null || sendRequest.amount == null || sendRequest.address == null) {
            System.out.println("Invalid request or input is null: " + WALLETS.get(fingerprint) +  sendRequest.amount);
            return ResponseEntity.badRequest().build();
        }

        sendCoinsToAddress(fingerprint, sendRequest.amount, sendRequest.address);

        return ResponseEntity.ok().build();
    }

    private void sendCoinsToAddress(String fingerprint, String amount, String address) {

        Wallet w = WALLETS.get(fingerprint);
        final Coin amountToSend = Coin.parseCoin(amount);
        final Address addressToSend = Address.fromBase58(PARAMS, address);

        SendRequest req = SendRequest.to(addressToSend, amountToSend);
        req.feePerKb = Coin.parseCoin("0.00001");

        Wallet.SendResult sendResult = null;

        try {
            System.out.println("About to send coins...");
            sendResult = w.sendCoins(SHARED_PEER_GROUP, req);
            System.out.println("Finished sending coins waiting on callback...");
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
                //walletStates.get(fingerprint).setBalance(wallets.get(fingerprint).wallet().getBalance().toFriendlyString());
                //sendWebWalletUpdate(fingerprint);
                System.out.println("Sent coins onwards! Transaction hash is " + finalSendResult.tx.getHashAsString());
            }
        }, new Executor() {
            @Override
            public void execute(Runnable command) {

            }
        });
    }

    private void addListeners(Wallet w, String fingerprint) {
        w.addCoinsReceivedEventListener(new WalletCoinsReceivedEventListener() {
            @Override
            public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {

                if(newBalance.isLessThan(prevBalance)) {
                    System.out.println("fingerprint: " + fingerprint + " received but new balanace is less than prev.. so actually sent..");
                    return;
                }

                System.out.println("fingerprint: " + fingerprint + " -----> coins RECEIVED: tx" + tx);

                DateTimeFormatter formatter4 = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
                String now = formatter4.format(LocalDateTime.now());

                String addressInTransaction = transactionExplorer.getFromAddress(tx, wallet) + " -> " + transactionExplorer.getSentToAddressFromOutput(tx, wallet);

                WalletTransaction transaction = WalletTransaction.builder()
                        .transactionType("received")
                        .transactionId(tx.getHashAsString())
                        .timestamp(now)
                        .address(addressInTransaction)
                        .amount(getFormattedBalance(newBalance.minus(prevBalance)))
                        //.debug("tx.getValueSentTome! :" + value.toFriendlyString() + " prevBalance: " + prevBalance.toFriendlyString() + " newBalance: " + newBalance.toFriendlyString() + "tx: " + tx)
                        .build();

                // Add to firest of list
                WALLET_STATES.get(fingerprint).getTransactions().add(0, transaction);
                WALLET_STATES.get(fingerprint).setBalance(getFormattedBalance(newBalance));
                sendWebWalletUpdate(fingerprint);
            }
        });

        w.addCoinsSentEventListener(new WalletCoinsSentEventListener() {
            @Override
            public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
                System.out.println("coins sent callback!");

                DateTimeFormatter formatter4 = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
                String now = formatter4.format(LocalDateTime.now());

                String addressInTransaction = transactionExplorer.getSentToAddress(tx, wallet, prevBalance.minus(newBalance).minus(Coin.parseCoin(".00001")));

                WalletTransaction transaction = WalletTransaction.builder()
                        .transactionType("sent")
                        .transactionId(tx.getHashAsString())
                        .timestamp(now)
                        .address(addressInTransaction)
                        .amount(getFormattedBalance(prevBalance.minus(newBalance)))
                        //.debug(transactionDebug + " !!!!!!! tx.getValueSentTome! :" + value.toFriendlyString() + "prevBalance: " + prevBalance.toFriendlyString() + " newBalance: " + newBalance.toFriendlyString() + "tx: " + tx)
                        .build();

                WALLET_STATES.get(fingerprint).getTransactions().add(0, transaction);
                WALLET_STATES.get(fingerprint).setBalance(getFormattedBalance(newBalance));
                sendWebWalletUpdate(fingerprint);
            }
        });

        w.addKeyChainEventListener(new KeyChainEventListener() {
            @Override
            public void onKeysAdded(List<ECKey> keys) {
                System.out.println("new key added");
            }
        });

        w.addScriptsChangeEventListener(new ScriptsChangeEventListener() {
            @Override
            public void onScriptsChanged(Wallet wallet, List<Script> scripts, boolean isAddingScripts) {
                System.out.println("new script added");
            }
        });

        w.addTransactionConfidenceEventListener(new TransactionConfidenceEventListener() {
            @Override
            public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx) {
//                System.out.println("-----> confidence changed: " + tx);
//                TransactionConfidence confidence = tx.getConfidence();
//                System.out.println("new block depth: " + confidence.getDepthInBlocks());
            }
        });
    }

    private void sendWebWalletUpdate(String fingerprint) {
        System.out.println("SENDING WEBSOCKET MESSAGE TO FINGERPRINT: " +fingerprint);
        if(WALLET_STATES.get(fingerprint) == null) {
            System.out.println("!!Was going to send websocket but wallet state was null, this can be a race condition on cold boot");
            return;
        }
        websocket.convertAndSend(WebSocketConfiguration.MESSAGE_PREFIX + "/updateWallet-" + fingerprint, WALLET_STATES.get(fingerprint));

    }

    private String getFormattedBalance(Coin balance) {
        return balance.toFriendlyString().replace("BTC","");
    }
}
