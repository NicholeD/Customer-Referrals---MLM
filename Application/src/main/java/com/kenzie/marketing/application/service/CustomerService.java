package com.kenzie.marketing.application.service;

import com.kenzie.marketing.application.controller.model.CreateCustomerRequest;
import com.kenzie.marketing.application.controller.model.CustomerResponse;
import com.kenzie.marketing.application.controller.model.LeaderboardUiEntry;
import com.kenzie.marketing.application.repositories.CustomerRepository;
import com.kenzie.marketing.application.repositories.model.CustomerRecord;
import com.kenzie.marketing.referral.model.*;
import com.kenzie.marketing.referral.model.client.ReferralServiceClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.UUID.randomUUID;

@Service
public class CustomerService {
    private static final Double REFERRAL_BONUS_FIRST_LEVEL = 10.0;
    private static final Double REFERRAL_BONUS_SECOND_LEVEL = 3.0;
    private static final Double REFERRAL_BONUS_THIRD_LEVEL = 1.0;

    private CustomerRepository customerRepository;
    private ReferralServiceClient referralServiceClient;

    public CustomerService(CustomerRepository customerRepository, ReferralServiceClient referralServiceClient) {
        this.customerRepository = customerRepository;
        this.referralServiceClient = referralServiceClient;
    }

    /**
     * findAllCustomers
     * @return A list of Customers
     */
    public List<CustomerResponse> findAllCustomers() {
        List<CustomerRecord> records = StreamSupport.stream(customerRepository.findAll().spliterator(), true)
                .collect(Collectors.toList());

        return records.stream()
                .map(this::toCustomerResponse)
                .collect(Collectors.toList());
    }

    /**
     * findByCustomerId
     * @param customerId
     * @return The Customer with the given customerId
     */
    public CustomerResponse getCustomer(String customerId) {
        Optional<CustomerRecord> record = customerRepository.findById(customerId);

        if (record.isEmpty()) {
            return null;
        }

        return record.stream()
                .map(this::toCustomerResponse)
                .findAny()
                .get();
    }

    /**
     * addNewCustomer
     *
     * This creates a new customer.  If the referrerId is included, the referrerId must be valid and have a
     * corresponding customer in the DB.  This posts the referrals to the referral service
     * @param createCustomerRequest
     * @return A CustomerResponse describing the customer
     */
    public CustomerResponse addNewCustomer(CreateCustomerRequest createCustomerRequest) {

        if (createCustomerRequest.getReferrerId().isPresent() && createCustomerRequest.getReferrerId().get().length() == 0) {
            createCustomerRequest.setReferrerId(Optional.empty());
        }

        CustomerRecord record = new CustomerRecord();
        record.setId(randomUUID().toString());
        record.setName(createCustomerRequest.getName());
        record.setDateCreated(LocalDateTime.now().toString());

        if (createCustomerRequest.getReferrerId().isPresent()) {
            record.setReferrerId(createCustomerRequest.getReferrerId().get());
        } else {
            record.setReferrerId(null);
        }

        referralServiceClient.addReferral(toReferralRequest(record));
        customerRepository.save(record);

        return toCustomerResponse(record);
    }

    /**
     * updateCustomer - This updates the customer name for the given customer id
     * @param customerId - The Id of the customer to update
     * @param customerName - The new name for the customer
     */
    public CustomerResponse updateCustomer(String customerId, String customerName) {
        Optional<CustomerRecord> customerExists = customerRepository.findById(customerId);
        if (customerExists.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer Not Found");
        }
        CustomerRecord customerRecord = customerExists.get();
        customerRecord.setName(customerName);
        customerRepository.save(customerRecord);

        return Optional.ofNullable(customerRecord)
                .stream()
                .map(this::toCustomerResponse)
                .findFirst()
                .get();
    }

    /**
     * deleteCustomer - This deletes the customer record for the given customer id
     * @param customerId
     */
    public void deleteCustomer(String customerId) {
        customerRepository.deleteById(customerId);
    }

    /**
     * calculateBonus - This calculates the referral bonus for the given customer according to the referral bonus
     * constants.
     * @param customerId
     * @return
     */
    public Double calculateBonus(String customerId) {
        CustomerReferrals referrals = referralServiceClient.getReferralSummary(customerId);

        Double calculationResult = REFERRAL_BONUS_FIRST_LEVEL * referrals.getNumFirstLevelReferrals() +
                REFERRAL_BONUS_SECOND_LEVEL * referrals.getNumSecondLevelReferrals() +
                REFERRAL_BONUS_THIRD_LEVEL * referrals.getNumThirdLevelReferrals();

        return calculationResult;
    }

    /**
     * getReferrals - This returns a list of referral entries for every customer directly referred by the given
     * customerId.
     * @param customerId
     * @return
     */
    public List<CustomerResponse> getReferrals(String customerId) {
        List<Referral> referrals = referralServiceClient.getDirectReferrals(customerId);

        return referrals.stream()
                .map(this::toCustomerRecord)
                .map(this::toCustomerResponse)
                .peek(response -> response.setReferrerName(getCustomer(customerId).getName()))
                .collect(Collectors.toList());
    }

    /**
     * getLeaderboard - This calls the referral service to retrieve the current top 5 leaderboard of the most referrals
     * @return
     */
    public List<LeaderboardUiEntry> getLeaderboard() {
        List<LeaderboardEntry> leaderBoardEntries = referralServiceClient.getLeaderboard();

        return leaderBoardEntries.stream()
                .filter(entry -> entry.getNumReferrals()>0)
                .map(this::toLBUiEntry)
                .collect(Collectors.toList());
    }

    /* -----------------------------------------------------------------------------------------------------------
        Private Methods
       ----------------------------------------------------------------------------------------------------------- */
    private CustomerResponse toCustomerResponse(CustomerRecord record) {
        if (record == null) {
            return null;
        }

        CustomerResponse customerResponse = new CustomerResponse(record);
        customerResponse.setId(record.getId());
        customerResponse.setName(record.getName());
        customerResponse.setDateJoined(record.getDateCreated());
        customerResponse.setReferrerId(record.getReferrerId());

        if (record.getReferrerId() != null) {
            customerResponse.setReferrerName(customerRepository.findById(record.getReferrerId()).get().getName());
        }

        return customerResponse;
    }

    private CustomerRecord toCustomerRecord(Referral referral) {
        if (referral == null) {
            return null;
        }

        CustomerRecord customerRecord = new CustomerRecord();
        customerRecord.setId(referral.getCustomerId());
        customerRecord.setDateCreated(referral.getReferralDate());
        customerRecord.setReferrerId(referral.getReferrerId());
        return customerRecord;
    }

    private ReferralRequest toReferralRequest(CustomerRecord record) {
        return new ReferralRequest(record.getId(), record.getReferrerId());
    }

    private LeaderboardUiEntry toLBUiEntry(LeaderboardEntry entry) {
        LeaderboardUiEntry lbUiEntry = new LeaderboardUiEntry();
        lbUiEntry.setCustomerId(entry.getCustomerId());
        lbUiEntry.setNumReferrals(entry.getNumReferrals());
        lbUiEntry.setCustomerName(getCustomer(entry.getCustomerId()).getName());

        return lbUiEntry;
    }

}
