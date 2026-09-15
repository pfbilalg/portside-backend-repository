package com.portside.trading.service;

import com.portside.trading.domain.*;
import com.portside.trading.repo.ContainerRepository;
import com.portside.trading.repo.ItemRepository;
import com.portside.trading.repo.PurchaseOrderRepository;
import com.portside.trading.repo.SupplierRepository;
import com.portside.trading.web.dto.PurchaseOrderRequest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ports the prototype's startPO()/savePO()/receivePO() (lines ~1163-1217). */
@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository poRepository;
    private final ContainerRepository containerRepository;
    private final SupplierRepository supplierRepository;
    private final ItemRepository itemRepository;
    private final SettingsService settingsService;
    private final CodeGeneratorService codeGenerator;

    public PurchaseOrderService(PurchaseOrderRepository poRepository, ContainerRepository containerRepository,
                                 SupplierRepository supplierRepository, ItemRepository itemRepository,
                                 SettingsService settingsService, CodeGeneratorService codeGenerator) {
        this.poRepository = poRepository;
        this.containerRepository = containerRepository;
        this.supplierRepository = supplierRepository;
        this.itemRepository = itemRepository;
        this.settingsService = settingsService;
        this.codeGenerator = codeGenerator;
    }

    @Transactional
    public PurchaseOrder create(PurchaseOrderRequest req) {
        PurchaseOrder po = PurchaseOrder.builder()
                .code(codeGenerator.nextPurchaseOrderCode())
                .status(PoStatus.DRAFT)
                .lines(new ArrayList<>())
                .build();
        applyRequest(po, req);
        return poRepository.save(po);
    }

    @Transactional
    public PurchaseOrder update(PurchaseOrder po, PurchaseOrderRequest req) {
        applyRequest(po, req);
        return poRepository.save(po);
    }

    private void applyRequest(PurchaseOrder po, PurchaseOrderRequest req) {
        po.setSupplier(supplierRepository.findByCode(req.supplierCode())
                .orElseThrow(() -> new IllegalArgumentException("No such supplier " + req.supplierCode())));
        po.setOrderDate(req.orderDate() == null ? LocalDate.now() : LocalDate.parse(req.orderDate()));
        po.setIncoterm(req.incoterm());
        po.setPaymentTerms(req.paymentTerms());
        po.setStatus(req.status() == null ? po.getStatus() : PoStatus.valueOf(req.status()));
        if (req.lines() != null) {
            po.getLines().clear();
            for (var l : req.lines()) {
                Item item = itemRepository.findByCode(l.itemCode())
                        .orElseThrow(() -> new IllegalArgumentException("No such item " + l.itemCode()));
                po.getLines().add(PurchaseOrderLine.builder().purchaseOrder(po).item(item).qty(l.qty())
                        .fobUnitPriceUsd(l.fobUnitPriceUsd()).build());
            }
        }
    }

    @Transactional
    public Container receive(PurchaseOrder po) {
        if (po.getContainer() != null) {
            throw new IllegalStateException(po.getCode() + " is already linked to container " + po.getContainer().getCode() + ".");
        }
        double usdRate = settingsService.usdRate();
        double fobPkr = po.getLines().stream().mapToDouble(l -> l.getQty() * l.getFobUnitPriceUsd()).sum() * usdRate;

        String code = codeGenerator.nextContainerCode();
        Container container = Container.builder()
                .code(code)
                .containerNo("NEW-" + code.substring(code.lastIndexOf('-') + 1))
                .supplier(po.getSupplier())
                .blNumber("BL-" + po.getCode())
                .gdNumber(null)
                .size("40' HC")
                .eta(LocalDate.now())
                .receivedDate(null)
                .status(ContainerStatus.IN_CLEARANCE)
                .currentBasis(AllocationBasis.VALUE)
                .lines(new ArrayList<>())
                .costs(new ArrayList<>())
                .build();
        for (var l : po.getLines()) {
            container.getLines().add(ContainerLine.builder().container(container).item(l.getItem())
                    .qty(l.getQty()).fobUnitPriceUsd(l.getFobUnitPriceUsd())
                    .weightKg(Math.round(l.getQty() * 3.2)).build());
        }
        List<ContainerCost> starterCosts = List.of(
                ContainerCost.builder().container(container).expenseHead("Ocean freight")
                        .amountPkr(Math.round(fobPkr * 0.09)).basis(AllocationBasis.OVERALL).build(),
                ContainerCost.builder().container(container).expenseHead("Marine insurance")
                        .amountPkr(Math.round(fobPkr * 0.016)).basis(AllocationBasis.VALUE).build(),
                ContainerCost.builder().container(container).expenseHead("Customs duty 20%")
                        .amountPkr(Math.round(fobPkr * 0.2)).basis(AllocationBasis.VALUE).build()
        );
        container.getCosts().addAll(starterCosts);
        container = containerRepository.save(container);

        po.setContainer(container);
        po.setStatus(PoStatus.RECEIVED);
        poRepository.save(po);
        return container;
    }
}
