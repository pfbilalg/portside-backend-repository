package com.portside.trading.repo;

import com.portside.trading.domain.ChartOfAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChartOfAccountRepository extends JpaRepository<ChartOfAccount, String> {
}
