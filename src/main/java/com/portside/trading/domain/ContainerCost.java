package com.portside.trading.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "container_cost")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContainerCost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "container_id", nullable = false)
    private Container container;

    @Column(nullable = false, length = 128)
    private String expenseHead;

    @Column(nullable = false)
    private double amountPkr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AllocationBasis basis;
}
