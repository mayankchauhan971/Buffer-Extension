/**
 * Swagger Redirect Controller
 * 
 * Simple controller that redirects the root URL to the Swagger UI documentation page.
 * Provides easy access to the API documentation interface for developers and users.
 */
package com.buffer.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SwaggerRedirectController {
    
    @GetMapping("/")
    public String redirectToSwagger() {
        return "redirect:/swagger-ui.html";
    }
}
