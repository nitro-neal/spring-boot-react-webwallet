package com.focus.springbootreact.webwallet;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController {

    @RequestMapping(value = "/")
    public String index() {
        // this will redirect to src/main/resources/templates/index.html
        return "index";
    }
}