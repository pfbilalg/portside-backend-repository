package com.portside.trading.repo;

import com.portside.trading.domain.StockEntryLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockEntryLineRepository extends JpaRepository<StockEntryLine, Long> {
    List<StockEntryLine> findByContainer_IdAndItem_Id(Long containerId, Long itemId);
}
