package com.bank.common.deserializers;

import com.bank.common.dto.contracts.accounts.BankOperation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import java.io.IOException;
import java.util.Arrays;

public class BankOperationDeserializer extends JsonDeserializer<BankOperation>
{
    @Override
    public BankOperation deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        String value = p.getText();
        try {
            return BankOperation.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidFormatException(p,
                "Invalid operation: " + value + ". Allowed: " + Arrays.toString(BankOperation.values()),
                value, BankOperation.class);
        }
    }
}
