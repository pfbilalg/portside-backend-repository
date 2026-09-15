package com.portside.trading.repo;

import com.portside.trading.domain.ReceiptAllocation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiptAllocationRepository extends JpaRepository<ReceiptAllocation, Long> {
    List<ReceiptAllocation> findByInvoice_Id(Long invoiceId);
}
