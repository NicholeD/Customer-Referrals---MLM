package com.kenzie.marketing.referral.service.task;

import com.kenzie.marketing.referral.model.LeaderboardEntry;
import com.kenzie.marketing.referral.service.ReferralService;
import com.kenzie.marketing.referral.service.model.ReferralRecord;

import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class ReferralTask implements Callable {
    private ReferralRecord record;
    private ReferralService referralService;

    public ReferralTask (ReferralRecord record, ReferralService referralService) {
        this.record = record;
        this.referralService = referralService;
    }
    @Override
    public List<LeaderboardEntry> call() throws Exception {
        TreeSet<LeaderboardEntry> top5ReferralTree = new TreeSet<>(Comparator.comparing(LeaderboardEntry::getNumReferrals).reversed());

        top5ReferralTree.add(new LeaderboardEntry(referralService.getCustomerReferralSummary(
                record.getCustomerId()).getNumFirstLevelReferrals(),
                record.getCustomerId()));

        return top5ReferralTree.stream()
                .limit(5)
                .collect(Collectors.toList());
    }
}
