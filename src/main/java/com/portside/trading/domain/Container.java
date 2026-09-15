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
@Table(name = "container")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Container {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** business lot reference, e.g. PSC-2601 */
    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 32)
    private String containerNo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(length = 32)
    private String blNumber;

    @Column(length = 32)
    private String gdNumber;

    @Column(nullable = false, length = 16)
    private String size;

    private LocalDate eta;

    private LocalDate receivedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ContainerStatus status;

    /** the allocation basis this lot's 'OVERALL' cost heads currently follow */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private AllocationBasis currentBasis = AllocationBasis.VALUE;

    @OneToMany(mappedBy = "container", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ContainerLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "container", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ContainerCost> costs = new ArrayList<>();

    public boolean isReceived() {
        return receivedDate != null;
    }
}
