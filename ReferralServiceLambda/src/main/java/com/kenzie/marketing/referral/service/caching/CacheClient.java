package com.kenzie.marketing.referral.service.caching;

import com.kenzie.marketing.referral.service.dependency.DaggerServiceComponent;

import redis.clients.jedis.Jedis;

import javax.inject.Inject;
import java.util.Optional;

public class CacheClient {

    @Inject
    public CacheClient() {}

    public void setValue(String key, int seconds, String value) {
        checkNonNullKey(key);

        try (Jedis cache = DaggerServiceComponent.create().provideJedis()) {
            cache.setex(key, seconds, value);
        };
    }
    public Optional<String> getValue(String key) {
        checkNonNullKey(key);

        try (Jedis cache = DaggerServiceComponent.create().provideJedis()) {
            return Optional.ofNullable(cache.get(key));
        }
    }
    public Boolean invalidate(String key) {
        checkNonNullKey(key);

        try (Jedis cache = DaggerServiceComponent.create().provideJedis()) {
            return cache.del(key) > 0;
        }
    }
    private void checkNonNullKey(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
    }
  }
}
