package com.example.databaseswebapp;

import com.example.databaseswebapp.database.Database;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.crypto.Data;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.SocketHandler;

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
    public String loginPost(@RequestParam("email") String email,
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


    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();

        for (Cookie cookie: cookies) {
            if(cookie.getName().equals("email")) {
                cookie.setMaxAge(0);
                response.addCookie(cookie);
                break;
            }
        }

        return "redirect:/";
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
        model.addAttribute("ingredients", Database.getUserIngredients(email));
        return "account";
    }

    @PostMapping("update-password")
    public String updatePassword(@RequestParam("currentPassword") String currentPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 Model model, HttpServletRequest request) {
        String email = getCookieValue(request, "email");
        if(newPassword.equals(confirmPassword)) {
            if(Database.changePassword(email, currentPassword, newPassword)) {
                return "redirect:/update-success";
            } else {
                return "redirect:/update-failure";
            }
        } else {
            return "redirect:/update-failure";
        }
    }

    @GetMapping("/update-success")
    public String updateSuccess(Model model) {
        model.addAttribute("message", "Success! You've updated your password!");
        return "message";
    }

    @GetMapping("/update-failure")
    public String updateFailure(Model model) {
        model.addAttribute("message", "Fail! An error occurred when attempted to change your password");
        return "message";
    }
    @PostMapping("/add-ingredient")
    public ResponseEntity<?> addIngredient(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        String ingredientName = payload.get("ingredient");
        String email = getCookieValue(request, "email");
        // Add logic to save the ingredient to the user's list
        boolean result = Database.insertIngredient(email, ingredientName);

        // Respond with JSON including the added ingredient
        if (result) {
            return ResponseEntity.ok(Map.of("success", true, "ingredient", ingredientName));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false));
        }
    }

    @DeleteMapping("/delete-ingredient/{name}")
    public ResponseEntity<?> deleteIngredient(@PathVariable("name") String name, HttpServletRequest request) {
        String email = getCookieValue(request, "email"); // Assuming this function retrieves the email from the cookie
        // Add logic to delete the ingredient from the user's list
        boolean result = Database.deleteIngredient(email, name); // Assuming this method handles deletion in the database

        // Respond with JSON based on the result
        if (result) {
            return ResponseEntity.ok(Map.of("success", true));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false));
        }
    }

    @GetMapping("/recipes")
    public String getRecipes(HttpServletRequest request, Model model) {
        String email = getCookieValue(request, "email");
        if(email != null) {
            model.addAttribute("email", email);
        }

        Recipe[] recipes = Database.getRecipes(email);
        model.addAttribute("recipes", recipes);
        return "recipes";
    }

    @GetMapping("/recipe/{id}")
    public String getRecipe(HttpServletRequest request, Model model, @PathVariable("id") String id) {
        String email = getCookieValue(request, "email");
        if(email != null) {
            model.addAttribute("email", email);
        }
        Recipe recipe = Database.getRecipe(id);
        if(recipe != null) {
            model.addAttribute("recipe", recipe);
        }
        Ingredient[] ingredients = Database.getRecipeIngredients(id, email);
        if(ingredients != null) {
            model.addAttribute("ingredients", ingredients);
        }
        return "recipe";
    }

    @GetMapping("add-a-recipe")
    public String addARecipe(HttpServletRequest request, Model model) {
        String email = getCookieValue(request, "email");
        if(email != null) {
            model.addAttribute("email", email);
        }
        return "add-a-recipe";
    }

    @PostMapping("add-a-recipe")
    public String postARecipe(@RequestParam("recipeTitle") String title,
                              @RequestParam("recipeImage") MultipartFile img,
                              @RequestParam("description") String desc,
                              @RequestParam("ingredientsList") String ingredientsJson) {
        try {
            String uploadDir = "C:\\Users\\austi\\OneDrive\\Desktop\\Java Projects\\DatabasesWebApp\\src\\main\\resources\\static\\images\\";
            File directory = new File(uploadDir);
            if(directory.exists()) {
               File file = new File(uploadDir + img.getOriginalFilename());
               img.transferTo(file);

               // After the image has been sucessfully created, we can store the recipe in the database
                java.util.List<String> ingredients = new ObjectMapper().readValue(ingredientsJson, new TypeReference<List<String>>() {});

                // Optionally, convert List to array if needed
                String[] ingredientsArray = ingredients.toArray(new String[0]);
                Database.createRecipe(title, desc, ingredientsArray, img.getOriginalFilename());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Handle img here if needed (e.g., save to disk, convert to another type)
        return "redirect:/recipes"; // Corrected redirect URL
    }
    @GetMapping("about")
    public String about(HttpServletRequest request, Model model) {
        String email = getCookieValue(request, "email");
        if(email != null) {
            model.addAttribute("email", email);
        }

        return "about";
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

