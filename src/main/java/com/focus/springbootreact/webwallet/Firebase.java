package com.focus.springbootreact.webwallet;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;


public class Firebase {

    private FirebaseApp app;
    private FirebaseDatabase database;
    private DatabaseReference databaseReference;

    public Firebase() {
        FileInputStream serviceAccount = null;

        try {
            serviceAccount = new FileInputStream("btc-testnet-wallet-firebase-firebase-adminsdk-huxqi-f41c5285ed.json");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        //https://btc-testnet-wallet-firebase.firebaseio.com

        FirebaseOptions options = null;
        try {
            options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://btc-testnet-wallet-firebase.firebaseio.com")
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.app = FirebaseApp.initializeApp(options);
        this.database = FirebaseDatabase.getInstance(app);
        this.databaseReference = database.getReference();
    }

    public FirebaseApp getApp() {
        return this.app;
    }

    public FirebaseDatabase getDatabase(FirebaseApp app) {
        return this.database;
    }

    public DatabaseReference getDatabaseReference() {
        return this.databaseReference;
    }
}
