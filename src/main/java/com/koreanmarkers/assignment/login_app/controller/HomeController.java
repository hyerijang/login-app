package com.koreanmarkers.assignment.login_app.controller;

import com.koreanmarkers.assignment.login_app.domain.User;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User user) {
                model.addAttribute("name", user.getNickname());
                if (user.getUserDetail() != null) {
                    model.addAttribute("email", user.getUserDetail().getEmail());
                }
                model.addAttribute("picture", user.getPictureUrl());
            }
        }
        return "index";
    }
}
