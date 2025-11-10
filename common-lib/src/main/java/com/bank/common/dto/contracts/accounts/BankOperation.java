package com.bank.common.dto.contracts.accounts;

import com.bank.common.exception.BusinessException;

import java.math.BigDecimal;

public enum BankOperation
{
    ADD {
        @Override
        public BigDecimal apply(BigDecimal x, BigDecimal y) {
            return x.add(y);
        }
    },
    SUBTRACT {
        @Override
        public BigDecimal apply(BigDecimal x, BigDecimal y) {
            if (x.compareTo(y) < 0) {
                throw new BusinessException("Insufficient balance");
            }
            return x.subtract(y);
        }
    }
    ;

    public abstract BigDecimal apply(BigDecimal x, BigDecimal y);
}
