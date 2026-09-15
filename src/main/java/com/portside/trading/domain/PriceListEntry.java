package com.portside.trading.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "price_list_entry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceListEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "item_id", nullable = false, unique = true)
    private Item item;

    @Column(nullable = false)
    private double listPrice;

    /** markup over unit cost that defines the approval floor, in percent */
    @Column(nullable = false)
    private double floorMarkupPct;

    @Column(nullable = false)
    private double tierBPct;

    @Column(nullable = false)
    private double tierCPct;

    @Column(nullable = false)
    private double tierDPct;
}
