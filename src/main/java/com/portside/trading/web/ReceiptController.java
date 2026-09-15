package com.portside.trading.web;

import com.portside.trading.repo.ReceiptRepository;
import com.portside.trading.service.ReceiptService;
import com.portside.trading.web.dto.ReceiptDto;
import com.portside.trading.web.dto.ReceiptRequest;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {

    private final ReceiptRepository receiptRepository;
    private final ReceiptService receiptService;

    public ReceiptController(ReceiptRepository receiptRepository, ReceiptService receiptService) {
        this.receiptRepository = receiptRepository;
        this.receiptService = receiptService;
    }

    @GetMapping
    @PreAuthorize("@accessService.canView('receipts')")
    @Transactional
    public List<ReceiptDto> list() {
        return receiptRepository.findAll().stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .map(ReceiptDto::of).toList();
    }

    @PostMapping
    @PreAuthorize("@accessService.canEdit('receipts')")
    @Transactional
    public ReceiptDto record(@RequestBody ReceiptRequest req) {
        try {
            return ReceiptDto.of(receiptService.record(req));
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }
}
