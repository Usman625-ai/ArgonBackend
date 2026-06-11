package com.ecommerce.multivendor.dto.response;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class AddressResponse {
    private Long id;
    private String fullName;
    private String phoneNumber;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String pincode;
    private String country;
    private boolean defaultAddress;
    private String fullAddress;
}
