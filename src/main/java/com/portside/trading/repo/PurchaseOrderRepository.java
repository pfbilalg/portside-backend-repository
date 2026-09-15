package com.portside.trading.repo;

import com.portside.trading.domain.PurchaseOrder;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    Optional<PurchaseOrder> findByCode(String code);

    long countByCodeStartingWith(String prefix);
}
