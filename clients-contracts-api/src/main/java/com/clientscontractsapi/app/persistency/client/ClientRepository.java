package com.clientscontractsapi.app.persistency.client;

import com.clientscontractsapi.app.models.client.entity.ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientRepository extends JpaRepository<ClientEntity, Long> {

    boolean existsByEmailIgnoreCase(String email);
}
