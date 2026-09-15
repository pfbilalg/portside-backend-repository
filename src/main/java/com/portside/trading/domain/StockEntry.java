package com.portside.trading.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stock_entry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private StockEntryType type;

    @Column(nullable = false, length = 128)
    private String postedBy;

    @OneToMany(mappedBy = "stockEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StockEntryLine> lines = new ArrayList<>();
}
