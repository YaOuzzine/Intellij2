package com.example.server_final_2;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/server-final2") // Note the name: server-final2, no hyphen in the middle
public class DemoController {

    @GetMapping("/welcome")
    public String welcomeMessage() {
        return "Welcome to Server 2!";
    }
}
