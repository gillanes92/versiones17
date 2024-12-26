package com.enel.rdd.azure.service;

import java.io.IOException;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/auth")
public class ReturnAzure {
	
	@GetMapping({ "/redirect" })
	public void callService(OAuth2AuthenticationToken token) {
		
		System.out.println(token.getName());
		
		
		
	}

}
