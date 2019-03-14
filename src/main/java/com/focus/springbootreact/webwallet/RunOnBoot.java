package com.focus.springbootreact.webwallet;

import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.SPVBlockStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;


@Component
public class RunOnBoot implements CommandLineRunner {

//    private static final File BLOCKCHAIN_FILE = new File("runonboot-block.dat");
//    private static final NetworkParameters NET_PARAMS = TestNet3Params.get();

    @Override
    public void run(String... args) throws Exception {
        APIController.RunOnBoot();

//
//
//            //List<Wallet> wallets = getWallets();
//            BlockStore blockStore = new SPVBlockStore(NET_PARAMS, BLOCKCHAIN_FILE);
//            BlockChain blockChain = new BlockChain(NET_PARAMS, wallets, blockStore);
//            PeerGroup peerGroup = new PeerGroup(NET_PARAMS, blockChain);
//
//            for (Wallet w : wallets) {
//                peerGroup.addWallet(w);
//            }
//
//            //Starting peerGroup;
//            peerGroup.startAsync();
//
//            //Start download blockchain
//            peerGroup.downloadBlockChain();
//        }
//
////        public static List<Wallet> getWallets() throws UnreadableWalletException {
////
////            List<Wallet> wallets = new ArrayList<>();
////            for (int i = 0; i < 5; i++) {
////                Wallet w = Wallet.loadFromFile(new File("wallet_" + i + ".dat"), null);
////                wallets.add(w);
////            }
////
////            return wallets;
////        }
    }
}