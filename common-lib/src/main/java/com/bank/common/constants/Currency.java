package com.bank.common.constants;

public enum Currency {
    RUB("Russian Ruble"),
    USD("US Dollar"),
    CNY("Chinese Yuan");

    private final String displayName;

    Currency(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
