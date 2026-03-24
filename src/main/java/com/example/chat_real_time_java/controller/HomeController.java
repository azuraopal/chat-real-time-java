package com.example.chat_real_time_java.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String redirectToChat() {
        return "redirect:/chat.html";
    }
}
