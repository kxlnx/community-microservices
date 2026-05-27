package com.nowcoder.user.controller;

import com.nowcoder.community.entity.User;
import com.nowcoder.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal")
public class UserInternalController {

    @Autowired
    private UserService userService;

    @GetMapping("/user/{id}")
    public User findUserById(@PathVariable("id") int id) {
        return userService.findUserById(id);
    }

    @GetMapping("/user/name/{name}")
    public User findUserByName(@PathVariable("name") String name) {
        return userService.findUserByName(name);
    }

    @PostMapping("/user/batch")
    public Map<Integer, User> findUsersByIds(@RequestBody List<Integer> ids) {
        return userService.findUsersByIds(ids);
    }

}
