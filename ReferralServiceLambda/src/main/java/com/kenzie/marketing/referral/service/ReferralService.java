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
        // Task 3 Code Here

        return null;
    }

    public CustomerReferrals getCustomerReferralSummary(String customerId) {
        CustomerReferrals referrals = new CustomerReferrals();
        List<ReferralRecord> firstReferralRecords = referralDao.findByReferrerId(customerId);
        Integer firstLevel = firstReferralRecords.size();
        Integer secondLevel = 0;
        Integer thirdLevel = 0;

        for (ReferralRecord firstLevelRecord : firstReferralRecords) {
            List<ReferralRecord> secondReferralRecords = referralDao.findByReferrerId(firstLevelRecord.getCustomerId());
            secondLevel += secondReferralRecords.size();
            for(ReferralRecord secondLevelRecord : secondReferralRecords) {
                List<ReferralRecord> thirdReferralRecords = referralDao.findByReferrerId(secondLevelRecord.getCustomerId());
                thirdLevel += thirdReferralRecords.size();
            }
        }

        referrals.setNumFirstLevelReferrals(firstLevel);
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
                .map(record -> referralConverter.fromRecordToReferral(record))
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
