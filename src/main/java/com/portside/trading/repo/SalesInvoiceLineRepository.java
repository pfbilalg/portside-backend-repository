package com.portside.trading.repo;

import com.portside.trading.domain.SalesInvoiceLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesInvoiceLineRepository extends JpaRepository<SalesInvoiceLine, Long> {
    List<SalesInvoiceLine> findByContainer_IdAndItem_Id(Long containerId, Long itemId);

    List<SalesInvoiceLine> findByContainer_Id(Long containerId);
}
