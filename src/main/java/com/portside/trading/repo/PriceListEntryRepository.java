package com.portside.trading.repo;

import com.portside.trading.domain.Item;
import com.portside.trading.domain.PriceListEntry;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceListEntryRepository extends JpaRepository<PriceListEntry, Long> {
    Optional<PriceListEntry> findByItem(Item item);

    Optional<PriceListEntry> findByItem_Code(String itemCode);
}
