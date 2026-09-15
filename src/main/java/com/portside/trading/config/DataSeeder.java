package com.portside.trading.config;

import com.portside.trading.domain.AppUser;
import com.portside.trading.domain.Role;
import com.portside.trading.repo.AppUserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates one dev login per role on first boot, since password hashes can't be embedded in the
 * Flyway seed SQL without a running PasswordEncoder. Dev password for every seed account:
 * "Portside@123" -- change before any non-local use.
 */
@Component
public class der implements ApplicationRunner {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private static final String DEV_PASSWORD = "Portside@123";

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) return;

        seed("owner", "Shahzad Rauf", Role.OWNER);
        seed("accountant", "Bilal Sheikh", Role.ACCOUNTANT);
        seed("sales", "Imran Qadri", Role.SALES_OFFICER);
        seed("storekeeper", "Rashid Mehmood", Role.STORE_KEEPER);
        seed("importdesk", "Faisal Rana", Role.IMPORT_DESK);
    }

    private void seed(String username, String fullName, Role role) {
        userRepository.save(AppUser.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(DEV_PASSWORD))
                .fullName(fullName)
                .role(role)
                .active(true)
                .build());
    }
}
