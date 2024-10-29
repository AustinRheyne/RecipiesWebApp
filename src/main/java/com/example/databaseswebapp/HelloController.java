package com.example.databaseswebapp;

import com.example.databaseswebapp.database.Database;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HelloController {


    @GetMapping("/")
    public String hello(Model model, HttpServletRequest request){
        String email = getCookieValue(request, "email");
        if(email != null) {
            model.addAttribute("email", email);
        }
        model.addAttribute("version", Database.getVerison());
        return "index";
    }

    @GetMapping("/sign-up")
    public String signUpGet(HttpServletRequest request) {
        String email = getCookieValue(request, "email");
        if(email == null) {
            return "sign-up";
        }
        return "redirect:/";
    }

    @PostMapping("/sign-up")
    public String signUpPost(@RequestParam("email") String email,
                             @RequestParam("password") String password) {
        Database.createNewUser(email, password);
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginGet(HttpServletRequest request) {
        String email = getCookieValue(request, "email");
        if(email == null) {
            return "login";
        }
        return "redirect:/";
    }

    @PostMapping("/login")
    public String LoginPost(@RequestParam("email") String email,
                            @RequestParam("password") String password,
                            HttpServletResponse response) {
        boolean success = Database.login(email, password);
        if(success) {
            Cookie loginCookie = new Cookie("email", email);
            // Keep users logged in for a week
            loginCookie.setMaxAge(60*60*24*7);
            response.addCookie(loginCookie);
            return "redirect:/";
        } else {
            return "redirect:/login-failure";
        }
    }

    @GetMapping("/login-success")
    public String loginSuccess(Model model) {
        model.addAttribute("message", "Success! You've been logged in!");
        return "message";
    }

    @GetMapping("/login-failure")
    public String loginFailure(Model model) {
        model.addAttribute("message", "Fail! The account you attempted to login to does not exist");
        return "message";
    }

    @GetMapping("/account")
    public String account(Model model, HttpServletRequest request) {

        String email = getCookieValue(request, "email");
        if(email == null) {
            return "redirect:/";
        }

        model.addAttribute("email", email);
        model.addAttribute("joinedDate", Database.getJoinDate(email));
        return "account";
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

}

