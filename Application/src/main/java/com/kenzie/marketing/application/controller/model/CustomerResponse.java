package com.kenzie.marketing.application.controller.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kenzie.marketing.application.repositories.model.CustomerRecord;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("dateJoined")
    private String dateJoined;

    @JsonProperty("referrerName")
    private String referrerName;

    @JsonProperty("referrerId")
    private String referrerId;

    public CustomerResponse(CustomerRecord record) {
        this.id = record.getId();
        this.name = record.getName();
        this.dateJoined = record.getDateCreated();
        this.referrerId = record.getReferrerId();

    }
    public CustomerResponse() {

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDateJoined() {
        return dateJoined;
    }

    public void setDateJoined(String dateJoined) {
        this.dateJoined = dateJoined;
    }

    public String getReferrerName() {
        return referrerName;
    }

    public void setReferrerName(String referrerName) {
        this.referrerName = referrerName;
    }

    public String getReferrerId() {
        return referrerId;
    }

    public void setReferrerId(String referrerId) {
        this.referrerId = referrerId;
    }
}
