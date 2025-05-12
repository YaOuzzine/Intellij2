package com.example.server_final;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/server-final")
public class DemoController {

    // Existing endpoints
    @GetMapping("/add")
    public int add(@RequestParam int num1, @RequestParam int num2) {
        return num1 + num2;
    }


    @GetMapping("/subtract")
    public int subtract(@RequestParam int num1, @RequestParam int num2) {
        return num1 - num2;
    }

    // New endpoint using POST
    @PostMapping("/multiply")
    public int multiply(@RequestBody MultiplicationRequest request) {
        return request.getNum1() * request.getNum2();
    }
}

// Request body class for multiplication
class MultiplicationRequest {
    private int num1;
    private int num2;

    // Getters and setters
    public int getNum1() {
        return num1;
    }

    public void setNum1(int num1) {
        this.num1 = num1;
    }

    public int getNum2() {
        return num2;
    }

    public void setNum2(int num2) {
        this.num2 = num2;
    }
}
