package com.ecommerce.multivendor.service.impl;

import com.ecommerce.multivendor.dto.request.AddressRequest;
import com.ecommerce.multivendor.dto.response.AddressResponse;
import com.ecommerce.multivendor.entity.Address;
import com.ecommerce.multivendor.entity.User;
import com.ecommerce.multivendor.exception.BadRequestException;
import com.ecommerce.multivendor.exception.ResourceNotFoundException;
import com.ecommerce.multivendor.exception.UnauthorizedException;
import com.ecommerce.multivendor.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AddressService {

    private final AddressRepository addressRepository;

    public AddressResponse addAddress(AddressRequest request, User user) {
        if (request.isDefaultAddress()) {
            addressRepository.clearDefaultAddress(user.getId());
        }
        // Auto-set first address as default
        boolean isFirst = addressRepository.findByUserId(user.getId()).isEmpty();

        Address address = Address.builder()
            .user(user)
            .fullName(request.getFullName())
            .phoneNumber(request.getPhoneNumber())
            .addressLine1(request.getAddressLine1())
            .addressLine2(request.getAddressLine2())
            .city(request.getCity())
            .state(request.getState())
            .pincode(request.getPincode())
            .country(request.getCountry() != null ? request.getCountry() : "India")
            .defaultAddress(request.isDefaultAddress() || isFirst)
            .build();

        return toAddressResponse(addressRepository.save(address));
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(Long userId) {
        return addressRepository.findByUserId(userId).stream()
            .map(this::toAddressResponse).toList();
    }

    public AddressResponse updateAddress(Long addressId, AddressRequest request, Long userId) {
        Address address = addressRepository.findById(addressId)
            .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));
        if (!address.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Address does not belong to you");
        }

        if (request.isDefaultAddress()) {
            addressRepository.clearDefaultAddress(userId);
        }

        address.setFullName(request.getFullName());
        address.setPhoneNumber(request.getPhoneNumber());
        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
        address.setCountry(request.getCountry() != null ? request.getCountry() : "India");
        address.setDefaultAddress(request.isDefaultAddress());

        return toAddressResponse(addressRepository.save(address));
    }

    public void deleteAddress(Long addressId, Long userId) {
        Address address = addressRepository.findById(addressId)
            .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));
        if (!address.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Address does not belong to you");
        }
        if (address.isDefaultAddress()) {
            throw new BadRequestException("Cannot delete default address. Set another as default first.");
        }
        addressRepository.delete(address);
    }

    private AddressResponse toAddressResponse(Address address) {
        return AddressResponse.builder()
            .id(address.getId())
            .fullName(address.getFullName())
            .phoneNumber(address.getPhoneNumber())
            .addressLine1(address.getAddressLine1())
            .addressLine2(address.getAddressLine2())
            .city(address.getCity())
            .state(address.getState())
            .pincode(address.getPincode())
            .country(address.getCountry())
            .defaultAddress(address.isDefaultAddress())
            .fullAddress(address.getFullAddress())
            .build();
    }
}
