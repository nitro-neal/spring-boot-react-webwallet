package com.focus.springbootreact.webwallet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.hateoas.EntityLinks;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;


// MAYBE NOT USED
@Component
@RepositoryEventHandler(WalletState.class)
public class EventHandler {

    private final SimpMessagingTemplate websocket;

    private final EntityLinks entityLinks;

    @Autowired
    public EventHandler(SimpMessagingTemplate websocket, EntityLinks entityLinks) {
        this.websocket = websocket;
        this.entityLinks = entityLinks;
    }

    @HandleAfterSave
    public void updateWalletResponse(WalletState walletResponse) {
        this.websocket.convertAndSend(WebSocketConfiguration.MESSAGE_PREFIX + "/updateWallet", walletResponse.getBalance());
    }

}