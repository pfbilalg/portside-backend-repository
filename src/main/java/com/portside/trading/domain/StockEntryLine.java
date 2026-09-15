package com.portside.trading.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stock_entry_line")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockEntryLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "stock_entry_id", nullable = false)
    private StockEntry stockEntry;

    @ManyToOne(optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(optional = false)
    @JoinColumn(name = "container_id", nullable = false)
    private Container container;

    /** positive receives stock in, negative writes it off */
    @Column(nullable = false)
    private double signedQty;

    @Column(length = 255)
    private String reason;

    /** unit cost snapshot at post time, from the container's landed cost worksheet */
    @Column(nullable = false)
    private double unitCostSnapshot;
}
