package com.example.elibrary.identity;

/**
 * The authenticated caller, derived exclusively from the bearer token. Application services never
 * accept a user id from request parameters or request bodies.
 */
public record Actor(String userId, boolean admin) {

    public Actor {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
    }

    public boolean canAccess(String ownerId) {
        return admin || userId.equals(ownerId);
    }
}
