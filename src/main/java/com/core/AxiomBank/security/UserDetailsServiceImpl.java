package com.core.AxiomBank.security;


import com.core.AxiomBank.Entities.Client;
import com.core.AxiomBank.Exceptions.NotFoundException;
import com.core.AxiomBank.Repositories.ClientRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements  UserDetailsService{

    private final ClientRepository clientRepository;

    public UserDetailsServiceImpl(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Client foundClient = clientRepository.findByEmail(username).orElseThrow(() ->
                new NotFoundException("Username not found"));
        return new UserDetailsImpl(foundClient);
    }
}
