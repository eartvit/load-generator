package org.concordia.eartvit.research;

import java.security.SecureRandom;

public class RandomStringBuilder {
    static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    SecureRandom rnd = null;

    private static RandomStringBuilder singleton = null;

    private RandomStringBuilder(){
        rnd = new SecureRandom();
    }

    public static RandomStringBuilder getInstance() {
        if (null == singleton){
            singleton = new RandomStringBuilder();
        }
        return singleton;
    }

    public String generateRandomString(int length){
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++){
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        }
        return sb.toString();
    }
}
