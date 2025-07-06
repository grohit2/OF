package com.roh.pfc.historicalload.models;

/**
 * This enumeration defines fields potentially shared across multiple reference ID types.
 */
public enum ReferenceIdField {
    SORT("sortId"),
    ACCOUNT("accountId");

    public final String id;

    ReferenceIdField(String id) {
        this.id = id;
    }
}