package com.portside.trading.repo;

import com.portside.trading.domain.Receipt;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    long countByCodeStartingWith(String prefix);

    List<Receipt> findByCustomer_Id(Long customerId);
}
