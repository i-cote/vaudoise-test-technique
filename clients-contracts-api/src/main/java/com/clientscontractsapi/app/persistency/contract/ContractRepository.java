package com.clientscontractsapi.app.persistency.contract;

import com.clientscontractsapi.app.models.contract.entity.ContractEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractRepository extends JpaRepository<ContractEntity, Long> {

    List<ContractEntity> findByClientId(Long clientId);

    @Query(
            "SELECT COALESCE(SUM(c.costAmount), 0) FROM ContractEntity c "
                    + "WHERE c.client.id = :clientId AND (c.endDate IS NULL OR c.endDate > :today)")
    BigDecimal sumActiveCostAmountByClient(
            @Param("clientId") Long clientId, @Param("today") LocalDate today);
}
