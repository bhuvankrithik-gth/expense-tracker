package com.ledgertracker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the public landing page at the root URL.
 * Authenticated users are sent on to the dashboard by the
 * client-side guard; everyone else sees the landing page with
 * Log In / Sign Up actions.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "forward:/home.html";
    }
}
