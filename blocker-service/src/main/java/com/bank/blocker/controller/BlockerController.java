package com.bank.blocker.controller;

import com.bank.common.dto.ApiResponse;
import com.bank.common.dto.contracts.blocker.BlockCheckRequest;
import com.bank.blocker.service.BlockerService;
import com.bank.common.dto.contracts.blocker.BlockCheckResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/blocker")
public class BlockerController {

    private final BlockerService blockerService;

    public BlockerController(BlockerService blockerService) {
        this.blockerService = blockerService;
    }

    @PostMapping("/check")
    public ApiResponse<BlockCheckResponse> checkOperation(@RequestBody BlockCheckRequest request) {
        boolean blocked = blockerService.checkOperation(request);
        return ApiResponse.success(BlockCheckResponse.of(blocked));
    }
}
