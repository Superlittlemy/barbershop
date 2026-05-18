package com.slm.barbershop.utils;

import com.slm.barbershop.model.AuthUser;

public final class UserContext {

    private static final ThreadLocal<AuthUser> THREAD_USER = new ThreadLocal<>();

    public static void setUser(AuthUser authUser) {
        THREAD_USER.set(authUser);
    }

    public static AuthUser getUser() {
        return THREAD_USER.get();
    }

    public static void clear() {
        THREAD_USER.remove();
    }

}