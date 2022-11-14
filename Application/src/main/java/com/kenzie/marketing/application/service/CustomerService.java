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
        List<CustomerRecord> records = StreamSupport.stream(customerRepository.findAll().spliterator(), true).collect(Collectors.toList());

        //Task 1 - Add your code here

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

        //Task 1 - Add your code here
        return record.map(this::toCustomerResponse).orElse(null);
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

        // TODO - Task 1 - Add your code here

        // TODO - 1.
        // There are two options for the referrerId - it's either empty (meaning the customer joined by themselves) or
        // contains a value (meaning another customer with that ID referred them). If the request contains a referrerId,
        // then that must be a valid customer id from the table. If that referrerId does not exist in the Customer table,
        // the request should be rejected.
        CustomerRecord record = new CustomerRecord();
        record.setId(randomUUID().toString());
        record.setName(createCustomerRequest.getName());
        record.setDateCreated(LocalDateTime.now().toString());

        if (createCustomerRequest.getReferrerId().isPresent()) {
            if (!customerRepository.findById(createCustomerRequest.getReferrerId().get()).isPresent()) {
                return null;
            }

            record.setReferrerId(createCustomerRequest.getReferrerId().get());
        }

        customerRepository.save(record);
        // TODO - 2.
        //The CustomerRecord should be created and saved into the Customer table.
        // To create the customer ID, use a call to record.setId(randomUUID().toString()).

        // TODO - 3.
        //A call should be made to referralServiceClient.addReferral() to add the new customer. It is important that
        // the referralServiceClient.addReferral() method is called for every customer added, even if they were not
        // referred! If a customer had no referrer, you should still call addReferral() using a blank referrerId.

        referralServiceClient.addReferral(toReferralRequest(record));

        // TODO - 4.
        //A response object should be created and returned with all the necessary information.

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

        //Task 1 - Add your code here

        return toCustomerResponse(customerRecord);
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

        //Task 1 - Add your code here
        List<Referral> referrals = referralServiceClient.getDirectReferrals(customerId);

        return referrals.stream()
                .map(r -> toCustomerRecord(r))
                .map(record -> toCustomerResponse(record))
                .peek(response -> response.setReferrerName(getCustomer(customerId).getName()))
                .collect(Collectors.toList());
    }

    /**
     * getLeaderboard - This calls the referral service to retrieve the current top 5 leaderboard of the most referrals
     * @return
     */
    public List<LeaderboardUiEntry> getLeaderboard() {

        // Task 2 - Add your code here

        return null;
    }

    /* -----------------------------------------------------------------------------------------------------------
        Private Methods
       ----------------------------------------------------------------------------------------------------------- */
    private CustomerResponse toCustomerResponse(CustomerRecord record) {
        if (record == null) {
            return null;
        }

        CustomerResponse customerResponse = new CustomerResponse();
        customerResponse.setName(record.getName());
        customerResponse.setId(record.getId());
        customerResponse.setReferrerId(record.getReferrerId());
        customerResponse.setDateJoined(record.getDateCreated());
        Optional<CustomerRecord> referrerRecord = customerRepository.findById(record.getReferrerId());
        customerResponse.setReferrerName(referrerRecord.get().getName());

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

}
