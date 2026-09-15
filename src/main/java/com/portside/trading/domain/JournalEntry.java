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
@Table(name = "journal_entry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String voucherNo;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 255)
    private String narration;

    /** e.g. SALES_INVOICE, RECEIPT, PO_RECEIVE */
    @Column(nullable = false, length = 32)
    private String sourceType;

    @Column(nullable = false, length = 32)
    private String sourceCode;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<JournalLine> lines = new ArrayList<>();
}
