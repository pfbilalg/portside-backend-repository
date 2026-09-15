package com.portside.trading.domain;

public enum AllocationBasis {
    /** follows the container's current cycling basis (value/weight/qty) */
    OVERALL,
    VALUE,
    WEIGHT,
    QUANTITY
}
