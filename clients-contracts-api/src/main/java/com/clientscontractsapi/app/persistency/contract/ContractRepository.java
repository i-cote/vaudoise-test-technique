package com.clientscontractsapi.app.persistency.contract;

import com.clientscontractsapi.app.models.contract.entity.ContractEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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

    @Query(
            "SELECT c FROM ContractEntity c "
                    + "WHERE c.client.id = :clientId "
                    + "AND (c.endDate IS NULL OR c.endDate > :today) "
                    + "ORDER BY c.startDate ASC, c.id ASC")
    List<ContractEntity> findActiveContractsByClient(
            @Param("clientId") Long clientId, @Param("today") LocalDate today);

    @Query(
            "SELECT c FROM ContractEntity c "
                    + "WHERE c.client.id = :clientId "
                    + "AND (c.endDate IS NULL OR c.endDate > :today) "
                    + "AND c.updatedAt >= :updatedSince "
                    + "ORDER BY c.startDate ASC, c.id ASC")
    List<ContractEntity> findActiveContractsByClientAndUpdatedSince(
            @Param("clientId") Long clientId,
            @Param("today") LocalDate today,
            @Param("updatedSince") OffsetDateTime updatedSince);
}
