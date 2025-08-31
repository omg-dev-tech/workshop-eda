package com.example.order;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class OrderController {


    @GetMapping("order")
    public String getMethodName(@RequestParam String param) {
        return new String();
    }
    
}
