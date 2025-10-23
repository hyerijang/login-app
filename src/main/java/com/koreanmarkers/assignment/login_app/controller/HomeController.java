package com.koreanmarkers.assignment.login_app.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

	@GetMapping("/")
	public String index(@AuthenticationPrincipal OAuth2User user, Model model) {
		if (user != null) {
			model.addAttribute("name", user.getAttribute("name"));
			model.addAttribute("email", user.getAttribute("email"));
			model.addAttribute("picture", user.getAttribute("picture"));
		}
		return "index";
	}
}




