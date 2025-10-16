package com.clientscontractsapi.app.persistency.contract;

import com.clientscontractsapi.app.models.contract.entity.ContractEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractRepository extends JpaRepository<ContractEntity, Long> {

    List<ContractEntity> findByClientId(Long clientId);
}
