package com.beautyShop.Opata.Website.entity;

public enum DeliveryStatus {
    REQUESTED,       // user just submitted the delivery request
    CONFIRMED,       // admin confirmed and accepted the delivery
    ASSIGNED,        // courier has been assigned
    PICKED_UP,       // courier picked up the package
    IN_TRANSIT,      // package is on the way
    OUT_FOR_DELIVERY,// courier is nearby, delivery imminent
    DELIVERED,       // successfully delivered
    FAILED,          // delivery attempt failed
    CANCELLED        // delivery was cancelled
}