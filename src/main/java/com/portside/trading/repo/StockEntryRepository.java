package com.portside.trading.repo;

import com.portside.trading.domain.StockEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockEntryRepository extends JpaRepository<StockEntry, Long> {
    long countByCodeStartingWith(String prefix);
}
