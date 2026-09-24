package com.shopeasy.auth.service;

import com.shopeasy.auth.dto.AddressRequest;
import com.shopeasy.auth.dto.AddressResponse;
import com.shopeasy.auth.entity.Address;
import com.shopeasy.auth.entity.User;
import com.shopeasy.auth.exception.ResourceNotFoundException;
import com.shopeasy.auth.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;

    @Transactional(readOnly = true)
    public List<AddressResponse> list(User user) {
        return addressRepository.findByUserOrderByIsDefaultDescCreatedAtDesc(user).stream()
                .map(AddressResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public AddressResponse create(User user, AddressRequest request) {
        boolean makeDefault = Boolean.TRUE.equals(request.getIsDefault()) || addressRepository.countByUser(user) == 0;

        if (makeDefault) {
            unsetExistingDefault(user);
        }

        Address address = Address.builder()
                .user(user)
                .fullName(request.getFullName().trim())
                .phone(request.getPhone().trim())
                .line1(request.getLine1())
                .line2(request.getLine2())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .country(request.getCountry() != null ? request.getCountry() : "India")
                .isDefault(makeDefault)
                .build();

        return AddressResponse.from(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse update(User user, Long id, AddressRequest request) {
        Address address = findOwned(user, id);

        if (Boolean.TRUE.equals(request.getIsDefault()) && !address.getIsDefault()) {
            unsetExistingDefault(user);
            address.setIsDefault(true);
        }

        address.setFullName(request.getFullName().trim());
        address.setPhone(request.getPhone().trim());
        address.setLine1(request.getLine1());
        address.setLine2(request.getLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
        if (request.getCountry() != null) address.setCountry(request.getCountry());

        return AddressResponse.from(addressRepository.save(address));
    }

    @Transactional
    public void delete(User user, Long id) {
        Address address = findOwned(user, id);
        addressRepository.delete(address);
    }

    @Transactional
    public AddressResponse setDefault(User user, Long id) {
        unsetExistingDefault(user);
        Address address = findOwned(user, id);
        address.setIsDefault(true);
        return AddressResponse.from(addressRepository.save(address));
    }

    private void unsetExistingDefault(User user) {
        addressRepository.findByUserOrderByIsDefaultDescCreatedAtDesc(user).stream()
                .filter(Address::getIsDefault)
                .forEach(a -> {
                    a.setIsDefault(false);
                    addressRepository.save(a);
                });
    }

    private Address findOwned(User user, Long id) {
        return addressRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
    }
}
