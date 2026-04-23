package com.expenseiq.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * PageController handles serving frontend pages.
 * 
 * OOP Concepts:
 * - ABSTRACTION: Users interact with clean URLs, internal routing is hidden
 * - ENCAPSULATION: Routing logic is contained within this controller
 * 
 * NOTE: This controller is only needed if you serve frontend FROM Spring Boot.
 * If frontend is deployed separately (Netlify/Vercel), this is optional.
 */
@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "forward:/index.html";
    }

    @GetMapping("/login")
    public String login() {
        return "forward:/auth.html";
    }

    @GetMapping("/register")
    public String register() {
        return "forward:/auth.html";
    }

    @GetMapping("/app")
    public String dashboard() {
        return "forward:/dashboard.html";
    }

    @GetMapping("/app/**")
    public String dashboardRoutes() {
        return "forward:/dashboard.html";
    }
}