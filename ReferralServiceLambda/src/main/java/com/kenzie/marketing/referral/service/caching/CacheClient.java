package com.kenzie.marketing.referral.service.caching;

import com.kenzie.marketing.referral.service.dependency.DaggerServiceComponent;

import redis.clients.jedis.Jedis;

import java.util.Optional;

public class CacheClient {

    public CacheClient() {}

    // Put your Cache Client Here

    public void setValue(String key, int seconds, String value) {
        // Check for non-null key
        checkNonNullKey(key);
        // Set the value in the cache
        Jedis cache = DaggerServiceComponent.create().provideJedis();
    }
    public Optional<String> getValue(String key) {
        // Check for non-null key
        checkNonNullKey(key);
        // Retrieves the Optional values from the cache
        try (Jedis cache = DaggerServiceComponent.create().provideJedis()) {
            return Optional.ofNullable(cache.get(key));
        }
    }
    public void invalidate(String key) {
        // Check for non-null key
        checkNonNullKey(key);
        // Delete the key
    }
    private void checkNonNullKey(String key) {
        // Ensure the key isn't null
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        // What should you do if the key *is* null?
    }

    // Since Jedis is being used multithreaded, you MUST get a new Jedis instances and close it inside every method.
    // Do NOT use a single instance across multiple of these methods

    // Use Jedis in each method by doing the following:
    // Jedis cache = DaggerServiceComponent.create().provideJedis();
    // ... use the cache
    // cache.close();

    // Remember to check for null keys!

    }
}
