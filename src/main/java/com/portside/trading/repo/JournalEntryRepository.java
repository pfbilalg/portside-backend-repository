package com.portside.trading.repo;

import com.portside.trading.domain.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    long countByVoucherNoStartingWith(String prefix);

    @Query("select je from JournalEntry je order by je.id desc")
    java.util.List<JournalEntry> findAllNewestFirst();
}
