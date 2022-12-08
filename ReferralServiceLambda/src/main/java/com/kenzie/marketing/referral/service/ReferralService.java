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

import javax.inject.Inject;

import java.util.Collections;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;
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
        TreeSet<LeaderboardEntry> top5Referrals = new TreeSet<>(Comparator.comparing(LeaderboardEntry::getNumReferrals));

        List<ReferralRecord> withoutReferrers = this.referralDao.findUsersWithoutReferrerId();

        for (ReferralRecord record : withoutReferrers) {
            LeaderboardEntry entry = new LeaderboardEntry(getDirectReferrals(record.getCustomerId()).size(),
                    record.getCustomerId());
            top5Referrals.add(entry);
        }

        return top5Referrals.stream()
                .limit(5)
                .collect(Collectors.toList());
    }

    public CustomerReferrals getCustomerReferralSummary(String customerId) {
        CustomerReferrals referrals = new CustomerReferrals();
        List<ReferralRecord> firstRefRecords = referralDao.findByReferrerId(customerId);
        referrals.setNumFirstLevelReferrals(firstRefRecords.size());
        int secondLevel = 0;
        int thirdLevel = 0;

        for (ReferralRecord firstRecord : firstRefRecords) {
            List<ReferralRecord> secondRefRecords = referralDao.findByReferrerId(firstRecord.getCustomerId());
            secondLevel += secondRefRecords.size();
            for(ReferralRecord secondRecord : secondRefRecords) {
                List<ReferralRecord> thirdRefRecords = referralDao.findByReferrerId(secondRecord.getCustomerId());
                thirdLevel += thirdRefRecords.size();
            }
        }

        referrals.setNumSecondLevelReferrals(secondLevel);
        referrals.setNumThirdLevelReferrals(thirdLevel);

        return referrals;
    }


    public List<Referral> getDirectReferrals(String customerId) {
        List<ReferralRecord> records = referralDao.findByReferrerId(customerId);

        // Task 1 Code Here
        // Find the referral records
        // Convert the records into referral objects
        // Return a list of referrals

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
