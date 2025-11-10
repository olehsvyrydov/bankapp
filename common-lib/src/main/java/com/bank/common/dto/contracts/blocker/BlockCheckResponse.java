
package com.bank.common.dto.contracts.blocker;

public record BlockCheckResponse(
    boolean blocked,
    String reason
)
{
    public static BlockCheckResponse of(boolean blocked)
    {
        return new BlockCheckResponse(blocked, blocked ? "Suspicious operation detected" : "Operation allowed");
    }
}
