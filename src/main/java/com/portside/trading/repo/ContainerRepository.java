package com.portside.trading.repo;

import com.portside.trading.domain.Container;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContainerRepository extends JpaRepository<Container, Long> {
    Optional<Container> findByCode(String code);
}
