package com.driver.services.impl;

import com.driver.model.Admin;
import com.driver.model.Country;
import com.driver.model.CountryName;
import com.driver.model.ServiceProvider;
import com.driver.repository.AdminRepository;
import com.driver.repository.CountryRepository;
import com.driver.repository.ServiceProviderRepository;
import com.driver.services.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AdminServiceImpl implements AdminService {
    @Autowired
    AdminRepository adminRepository1;

    @Autowired
    ServiceProviderRepository serviceProviderRepository1;

    @Autowired
    CountryRepository countryRepository1;

    @Override
    public Admin register(String username, String password) {
        Admin admin = new Admin();
        admin.setUsername(username);
        admin.setPassword(password);
        admin.setServiceProviders(new ArrayList<>());
        adminRepository1.save(admin);
        return admin;
    }

    @Override
    public Admin addServiceProvider(int adminId, String providerName) {
        Optional<Admin> optionalAdmin = adminRepository1.findById(adminId);
        Admin admin = optionalAdmin.get();

        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setAdmin(admin);
        serviceProvider.setName(providerName);

        List<ServiceProvider> serviceProviderList = admin.getServiceProviders();
        serviceProviderList.add(serviceProvider);
        admin.setServiceProviders(serviceProviderList);

        adminRepository1.save(admin);
        return admin;
    }

    @Override
    public ServiceProvider addCountry(int serviceProviderId, String countryName) throws Exception{
        Optional<ServiceProvider> optionalServiceProvider = serviceProviderRepository1.findById(serviceProviderId);
        if(!optionalServiceProvider.isPresent()) throw new Exception("Service provider not found");

        ServiceProvider serviceProvider = optionalServiceProvider.get();
        Country country = new Country();
        country.enrich(countryName);

        country.setServiceProvider(serviceProvider);
        //Country countryWithId = countryRepository1.save(country);

        List<Country> countryList = serviceProvider.getCountryList();
        countryList.add(country);
        serviceProvider.setCountryList(countryList);

        serviceProviderRepository1.save(serviceProvider);
        return serviceProvider;
    }
}
