package com.driver.services.impl;

import com.driver.model.*;
import com.driver.repository.ConnectionRepository;
import com.driver.repository.ServiceProviderRepository;
import com.driver.repository.UserRepository;
import com.driver.services.ConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ConnectionServiceImpl implements ConnectionService {
    @Autowired
    UserRepository userRepository2;
    @Autowired
    ServiceProviderRepository serviceProviderRepository2;
    @Autowired
    ConnectionRepository connectionRepository2;

    @Override
    public User connect(int userId, String countryName) throws Exception{
        Optional<User> optionalUser = userRepository2.findById(userId);
        User user = optionalUser.get();

        if(user.getConnected() == true || user.getMaskedIp() != null) {
            throw new Exception("Already connected");
        }

        Country givenCountry = new Country();
        givenCountry.enrich(countryName);

        if(user.getOriginalCountry().getCountryName().equals(givenCountry.getCountryName())) {
            return user;
        }
        else {
            List<ServiceProvider> serviceProviderList = user.getServiceProviderList();

            Integer minId = null;
            ServiceProvider sp_minId = null;

            for(ServiceProvider serviceProvider : serviceProviderList) {
                List<Country> sp_countryList = serviceProvider.getCountryList();
                int sp_Id = serviceProvider.getId();

                for(Country country : sp_countryList) {
                    if(country.getCode().equals(givenCountry.getCode())) {

                        //try to choose service provider
                        if(sp_minId == null || minId > sp_Id) {
                            sp_minId = serviceProvider;
                            minId = sp_Id;
                        }
                    }
                }
            }

            if(sp_minId == null) {
                //did not find any suitable subscribed service provider
                throw new Exception("Unable to connect");
            }

            Connection connection = new Connection();

            connection.setUser(user);
            user.setConnected(true);
            List<Connection> userConnection = user.getConnectionList();
            userConnection.add(connection);
            user.setConnectionList(userConnection);
            user.setMaskedIp(givenCountry.getCode() + "." + sp_minId.getId() + "." + user.getId());
            user.setVpnCountry(givenCountry);

            connection.setServiceProvider(sp_minId);
            List<Connection> spConnectionList = sp_minId.getConnectionList();
            spConnectionList.add(connection);
            sp_minId.setConnectionList(spConnectionList);

            userRepository2.save(user);
            serviceProviderRepository2.save(sp_minId);

            //no service provider offers vpn to the country
        }
        return user;
    }
    @Override
    public User disconnect(int userId) throws Exception {
        Optional<User> optionalUser = userRepository2.findById(userId);
        User user = optionalUser.get();

        if(user.getConnected() == false) {
            throw new Exception("Already disconnected");
        }

        user.setMaskedIp(null);
        user.setConnected(false);
        userRepository2.save(user);

        return user;
    }
    @Override
    public User communicate(int senderId, int receiverId) throws Exception {
        Optional<User> optionalSender = userRepository2.findById(senderId);
        if(!optionalSender.isPresent()) throw new Exception("Sender not found");

        Optional<User> optionalReceiver = userRepository2.findById(receiverId);
        if(!optionalReceiver.isPresent()) throw new Exception("Receiver not found");

        User sender = optionalSender.get();
        User receiver = optionalReceiver.get();

        CountryName receiverCountryName = null;
        if(receiver.getConnected() == true || receiver.getMaskedIp() != null) {
            String maskedCode = receiver.getMaskedIp().substring(0, 3);
            if(maskedCode.equals("001")) receiverCountryName = CountryName.IND;
            else if(maskedCode.equals("002")) receiverCountryName = CountryName.USA;
            else if(maskedCode.equals("003")) receiverCountryName = CountryName.AUS;
            else if(maskedCode.equals("004")) receiverCountryName = CountryName.CHI;
            else if(maskedCode.equals("005")) receiverCountryName = CountryName.JPN;
        } else
            receiverCountryName = receiver.getOriginalCountry().getCountryName();


        //find suitable vpn for sender
        try {
            connect(senderId, receiverCountryName.toString());
            return sender;
        } catch(Exception e) {
            throw new Exception("Cannot establish communication");
        }
    }
}
