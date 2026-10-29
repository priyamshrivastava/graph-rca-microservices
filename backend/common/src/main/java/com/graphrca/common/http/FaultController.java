package com.graphrca.common.http;

import com.graphrca.common.dto.FaultRequest;
import com.graphrca.common.dto.FaultResponse;
import com.graphrca.common.fault.FaultState;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fault")
public class FaultController {
    private final FaultState faultState;

    public FaultController(FaultState faultState) {
        this.faultState = faultState;
    }

    @PostMapping
    public FaultResponse inject(@Valid @RequestBody FaultRequest request) {
        return faultState.activate(request);
    }
}
