package com.focus.springbootreact.webwallet;

import org.springframework.web.bind.annotation.*;

@RestController
public class APIController {

    @RequestMapping("/initwallet")
    public WalletResponse initwallet() {
        return new WalletResponse("init wallet called");
    }

    @RequestMapping(value = "/setfingerprint", method = RequestMethod.POST)
    public WalletResponse setfingerprint(@RequestBody Fingerprint fingerprint) {
        System.out.println("Fingerprint: " + fingerprint.fingerprint);
        return new WalletResponse("fingerprint set");
    }
}
