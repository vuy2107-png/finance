package com.codegym.finance.service;

import com.codegym.finance.entity.User;

public interface IUserService {
    void save(User user);
    boolean existsByUsername(String username);
}
