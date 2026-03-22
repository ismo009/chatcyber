package com.chatcyber.crypto;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Manages 6-digit verification codes for email validation before exchanging private keys.
 * Codes expire after 10 minutes.
 */
public class VerificationCodeManager {
    
    private static final int CODE_LENGTH = 6;
    private static final long EXPIRATION_TIME_MS = 10 * 60 * 1000; // 10 minutes
    private final SecureRandom random = new SecureRandom();
    
    private final Map<String, CodeEntry> codes = new HashMap<>();
    private final Timer cleanupTimer = new Timer("VerificationCodeCleanup", true);
    
    private static class CodeEntry {
        final String code;
        final long createdAt;
        
        CodeEntry(String code) {
            this.code = code;
            this.createdAt = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - createdAt > EXPIRATION_TIME_MS;
        }
    }
    
    public VerificationCodeManager() {
        // Schedule cleanup of expired codes every minute
        cleanupTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                cleanupExpiredCodes();
            }
        }, EXPIRATION_TIME_MS, EXPIRATION_TIME_MS);
    }
    
    /**
     * Generate a new 6-digit verification code for the given identity (email).
     * If a valid code already exists, it will be replaced.
     * 
     * @param identity the email address
     * @return the generated 6-digit code
     */
    public synchronized String generateCode(String identity) {
        String normalizedIdentity = normalizeIdentity(identity);
        
        // Generate a random 6-digit code
        int codeInt = 100000 + random.nextInt(900000);
        String code = String.valueOf(codeInt);
        
        codes.put(normalizedIdentity, new CodeEntry(code));
        return code;
    }
    
    /**
     * Verify if the provided code matches the one issued for this identity.
     * If valid, the code is consumed (removed).
     * 
     * @param identity the email address
     * @param providedCode the code provided by the user
     * @return true if the code is valid and not expired, false otherwise
     */
    public synchronized boolean verifyCode(String identity, String providedCode) {
        String normalizedIdentity = normalizeIdentity(identity);
        
        CodeEntry entry = codes.get(normalizedIdentity);
        if (entry == null) {
            return false;
        }
        
        if (entry.isExpired()) {
            codes.remove(normalizedIdentity);
            return false;
        }
        
        if (entry.code.equals(providedCode)) {
            codes.remove(normalizedIdentity); // Consume the code
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if a valid (non-expired) code exists for this identity.
     * 
     * @param identity the email address
     * @return true if a valid code exists
     */
    public synchronized boolean hasValidCode(String identity) {
        String normalizedIdentity = normalizeIdentity(identity);
        CodeEntry entry = codes.get(normalizedIdentity);
        return entry != null && !entry.isExpired();
    }
    
    /**
     * Get the verification code for debugging purposes (should not be used in production UI).
     * 
     * @param identity the email address
     * @return the code or null if not found
     */
    public synchronized String getCodeForDebug(String identity) {
        String normalizedIdentity = normalizeIdentity(identity);
        CodeEntry entry = codes.get(normalizedIdentity);
        return entry != null && !entry.isExpired() ? entry.code : null;
    }
    
    private synchronized void cleanupExpiredCodes() {
        codes.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    private String normalizeIdentity(String identity) {
        return identity == null ? "" : identity.toLowerCase().trim();
    }
    
    public void shutdown() {
        cleanupTimer.cancel();
    }
}
