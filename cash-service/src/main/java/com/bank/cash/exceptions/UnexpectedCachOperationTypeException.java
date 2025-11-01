
package com.bank.cash.exceptions;

public class UnexpectedCachOperationTypeException extends RuntimeException
{
    public UnexpectedCachOperationTypeException(String message)
    {
        super(message);
    }
}
