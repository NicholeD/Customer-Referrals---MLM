package com.kenzie.marketing.referral.service.caching;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.kenzie.marketing.referral.service.dao.NonCachingReferralDao;
import com.kenzie.marketing.referral.service.dao.ReferralDao;
import com.kenzie.marketing.referral.service.model.ReferralRecord;

import javax.inject.Inject;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class CachingReferralDao implements ReferralDao {

    private static final int REFERRAL_READ_TTL = 60 * 60;
    private static final String REFERRAL_KEY = "ReferralKey::%s";

    private final CacheClient cacheClient;
    private final NonCachingReferralDao nonCachingReferralDao;
    private final Gson gson;

    @Inject
    public CachingReferralDao(CacheClient cacheClient, NonCachingReferralDao nonCachingReferralDao) {
        this.cacheClient = cacheClient;
        this.nonCachingReferralDao = nonCachingReferralDao;

        this.gson = new GsonBuilder().registerTypeAdapter(
                ZonedDateTime.class,
                new TypeAdapter<ZonedDateTime>() {
                    @Override
                    public void write(JsonWriter out, ZonedDateTime value) throws IOException {
                        out.value(value.toString());
                    }
                    @Override
                    public ZonedDateTime read(JsonReader in) throws IOException {
                        return ZonedDateTime.parse(in.nextString());
                    }
                }
        ).enableComplexMapKeySerialization().create();

    }

    @Override
    public ReferralRecord addReferral(ReferralRecord referral) {
        // Invalidate
        cacheClient.invalidate(String.format(REFERRAL_KEY, referral.getReferrerId()));
        // Add referral to database
        return nonCachingReferralDao.addReferral(referral);
    }

    @Override
    public List<ReferralRecord> findByReferrerId(String referrerId) {
        // Look up data in cache
        List<ReferralRecord> refRecords = new ArrayList<>();

        cacheClient.getValue(String.format(REFERRAL_KEY, referrerId)).ifPresentOrElse(string -> refRecords.addAll(fromJson(string)),
                () -> refRecords.addAll(addToCache(nonCachingReferralDao.findByReferrerId(referrerId), referrerId)));
        // Convert between JSON
        // If the data doesn't exist in the cache,
        // Get the data from the data source
        // Add data to the cache, convert between JSON
        return refRecords;
    }

    @Override
    public List<ReferralRecord> findUsersWithoutReferrerId() {
        // Look up customer from the data source
        return nonCachingReferralDao.findUsersWithoutReferrerId();
    }

    // Converting out of the cache
    private List<ReferralRecord> fromJson(String json) {
        return gson.fromJson(json, new TypeToken<ArrayList<ReferralRecord>>() { }.getType());
    }
    // Setting value
    private List<ReferralRecord> addToCache(List<ReferralRecord> records, String referrerId) {
        cacheClient.setValue(String.format(REFERRAL_KEY, referrerId),
                REFERRAL_READ_TTL, gson.toJson(records)
        );
        return records;
    }
}
