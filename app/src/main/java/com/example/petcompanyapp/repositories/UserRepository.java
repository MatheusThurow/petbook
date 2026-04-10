package com.example.petcompanyapp.repositories;

import com.example.petcompanyapp.models.User;
import com.example.petcompanyapp.utils.UserType;

import java.util.ArrayList;
import java.util.List;

public final class UserRepository {

    private static final List<User> USERS = new ArrayList<>();
    private static long nextId = 3L;

    static {
        USERS.add(new User(
                1L,
                UserType.PERSON,
                "Ana Souza",
                "ana@petcompany.com",
                "123456",
                "12345678901",
                true
        ));

        USERS.add(new User(
                2L,
                UserType.COMPANY,
                "Clinica Feliz",
                "contato@clinicafeliz.com",
                "123456",
                "12345678000199",
                true
        ));
    }

    private UserRepository() {
    }

    public static User authenticate(String email, String password, String userType) {
        for (User user : USERS) {
            if (same(email, user.getEmail())
                    && same(password, user.getPassword())
                    && same(userType, user.getUserType())
                    && user.isActive()) {
                return user;
            }
        }
        return null;
    }

    public static User register(
            String userType,
            String name,
            String email,
            String password,
            String document
    ) {
        if (findByEmail(email) != null) {
            return null;
        }

        User user = new User(
                nextId++,
                userType,
                name,
                email,
                password,
                document,
                true
        );
        USERS.add(user);
        return user;
    }

    public static User findById(Long userId) {
        if (userId == null) {
            return null;
        }

        for (User user : USERS) {
            if (userId.equals(user.getId())) {
                return user;
            }
        }
        return null;
    }

    public static User findByEmail(String email) {
        for (User user : USERS) {
            if (same(email, user.getEmail())) {
                return user;
            }
        }
        return null;
    }

    public static boolean isValidActiveUser(Long userId) {
        User user = findById(userId);
        return user != null && user.isActive();
    }

    public static boolean resetPassword(String email, String newPassword) {
        for (int i = 0; i < USERS.size(); i++) {
            User user = USERS.get(i);

            if (same(email, user.getEmail())) {

                User updatedUser = new User(
                        user.getId(),
                        user.getUserType(),
                        user.getName(),
                        user.getEmail(),
                        newPassword,
                        user.getDocument(),
                        user.isActive()
                );

                USERS.set(i, updatedUser);
                return true;
            }
        }
        return false;
    }

    private static boolean same(String left, String right) {
        return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
    }
}