package com.portside.trading.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 64)
    private String city;

    @Column(nullable = false, length = 32)
    private String paymentTerms;

    @Column(nullable = false)
    private double creditLimit;

    @Column(nullable = false)
    private double standingDiscountPct;

    @Column(nullable = false, length = 128)
    private String salesman;
}
