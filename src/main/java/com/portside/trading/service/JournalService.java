package com.portside.trading.service;

import com.portside.trading.domain.ChartOfAccount;
import com.portside.trading.domain.JournalEntry;
import com.portside.trading.domain.JournalLine;
import com.portside.trading.repo.ChartOfAccountRepository;
import com.portside.trading.repo.JournalEntryRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class JournalService {

    public record Posting(String accountCode, double debit, double credit) {
        public static Posting debit(String account, double amount) { return new Posting(account, amount, 0); }
        public static Posting credit(String account, double amount) { return new Posting(account, 0, amount); }
    }

    private final JournalEntryRepository journalEntryRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;
    private final CodeGeneratorService codeGenerator;

    public JournalService(JournalEntryRepository journalEntryRepository,
                           ChartOfAccountRepository chartOfAccountRepository,
                           CodeGeneratorService codeGenerator) {
        this.journalEntryRepository = journalEntryRepository;
        this.chartOfAccountRepository = chartOfAccountRepository;
        this.codeGenerator = codeGenerator;
    }

    public JournalEntry post(String voucherPrefix, LocalDate date, String narration, String sourceType,
                              String sourceCode, List<Posting> postings) {
        JournalEntry entry = JournalEntry.builder()
                .voucherNo(codeGenerator.nextVoucherNo(voucherPrefix))
                .date(date)
                .narration(narration)
                .sourceType(sourceType)
                .sourceCode(sourceCode)
                .build();
        List<JournalLine> lines = new ArrayList<>();
        for (Posting p : postings) {
            ChartOfAccount account = chartOfAccountRepository.findById(p.accountCode())
                    .orElseThrow(() -> new IllegalStateException("Unknown account " + p.accountCode()));
            lines.add(JournalLine.builder().journalEntry(entry).account(account).debit(p.debit()).credit(p.credit()).build());
        }
        entry.setLines(lines);
        return journalEntryRepository.save(entry);
    }
}
