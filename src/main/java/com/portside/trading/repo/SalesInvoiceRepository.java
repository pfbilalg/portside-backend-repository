package com.portside.trading.repo;

import com.portside.trading.domain.SalesInvoice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesInvoiceRepository extends JpaRepository<SalesInvoice, Long> {
    Optional<SalesInvoice> findByCode(String code);

    long countByCodeStartingWith(String prefix);

    List<SalesInvoice> findByCustomer_Id(Long customerId);
}
