package com.element.analytics.commonUtils;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class WebPageController {
	
	@RequestMapping("/")
	public String homePage(Model model) {
		return "Login";
	}

}
