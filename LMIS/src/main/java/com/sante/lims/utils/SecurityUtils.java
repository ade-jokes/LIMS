package com.sante.lims.utils;

import org.mindrot.jbcrypt.BCrypt;

public class SecurityUtils {

    /**
     * Hashes a password using BCrypt.
     */
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    /**
     * Checks if a plain password matches a BCrypt hash.
     */
    public static boolean checkPassword(String password, String hashed) {
        if (password == null || hashed == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(password, hashed);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
