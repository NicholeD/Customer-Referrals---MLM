package com.kenzie.marketing.referral.service;

import com.kenzie.marketing.referral.model.CustomerReferrals;
import com.kenzie.marketing.referral.model.LeaderboardEntry;
import com.kenzie.marketing.referral.model.Referral;
import com.kenzie.marketing.referral.model.ReferralRequest;
import com.kenzie.marketing.referral.model.ReferralResponse;
import com.kenzie.marketing.referral.service.converter.ReferralConverter;
import com.kenzie.marketing.referral.service.dao.ReferralDao;
import com.kenzie.marketing.referral.service.exceptions.InvalidDataException;
import com.kenzie.marketing.referral.service.model.ReferralRecord;
import com.kenzie.marketing.referral.service.task.ReferralTask;

import javax.inject.Inject;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ReferralService {

    private ReferralDao referralDao;
    private ExecutorService executor;
    private ReferralConverter referralConverter = new ReferralConverter();

    @Inject
    public ReferralService(ReferralDao referralDao) {
        this.referralDao = referralDao;
        this.executor = Executors.newCachedThreadPool();
    }

    // Necessary for testing, do not delete
    public ReferralService(ReferralDao referralDao, ExecutorService executor) {
        this.referralDao = referralDao;
        this.executor = executor;
    }

    public List<LeaderboardEntry> getReferralLeaderboard() {
        List<ReferralRecord> withoutReferrers = referralDao.findUsersWithoutReferrerId();

        List<Future<List<LeaderboardEntry>>> threadFutures = new ArrayList<>();

        TreeSet<LeaderboardEntry> top5ReferralTree = new TreeSet<>(Comparator.comparingInt(LeaderboardEntry::getNumReferrals).reversed());

        for(ReferralRecord record : withoutReferrers) {
            ReferralTask task = new ReferralTask(record, this);
            threadFutures.add(executor.submit(task));
        }

        executor.shutdown();

        try {
            executor.awaitTermination(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException("Executor was interrupted " + e);
        }

        return top5ReferralTree.stream()
                .limit(5)
                .collect(Collectors.toList());
    }

    public CustomerReferrals getCustomerReferralSummary(String customerId) {
        CustomerReferrals referrals = new CustomerReferrals();
        List<ReferralRecord> firstRefRecords = referralDao.findByReferrerId(customerId);
        referrals.setNumFirstLevelReferrals(firstRefRecords.size());

        List<Referral> secondRefRecords = new ArrayList<>();
        List<Referral> thirdRefRecords = new ArrayList<>();

        for (ReferralRecord firstRecord : firstRefRecords) {
            secondRefRecords.addAll(getDirectReferrals(firstRecord.getCustomerId()));
        }

        for(Referral secondRef : secondRefRecords) {
            thirdRefRecords.addAll(getDirectReferrals(secondRef.getCustomerId()));
        }

        referrals.setNumSecondLevelReferrals(secondRefRecords.size());
        referrals.setNumThirdLevelReferrals(thirdRefRecords.size());

        return referrals;
    }

    public List<Referral> getDirectReferrals(String customerId) {
        List<ReferralRecord> records = referralDao.findByReferrerId(customerId);

        return records.stream()
                .map(ReferralConverter::fromRecordToReferral)
                .collect(Collectors.toList());
    }


    public ReferralResponse addReferral(ReferralRequest referral) {
        if (referral == null || referral.getCustomerId() == null || referral.getCustomerId().length() == 0) {
            throw new InvalidDataException("Request must contain a valid Customer ID");
        }
        ReferralRecord record = ReferralConverter.fromRequestToRecord(referral);
        referralDao.addReferral(record);
        return ReferralConverter.fromRecordToResponse(record);
    }
}
