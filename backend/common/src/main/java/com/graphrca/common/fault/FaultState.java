package com.graphrca.common.fault;

import com.graphrca.common.dto.FaultRequest;
import com.graphrca.common.dto.FaultResponse;
import com.graphrca.common.dto.FaultType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class FaultState {
    private final AtomicReference<FaultResponse> activeFault = new AtomicReference<>();

    public FaultResponse activate(FaultRequest request) {
        FaultResponse fault = new FaultResponse(
                request.type(),
                request.magnitude(),
                Instant.now().plusSeconds(request.durationSec())
        );
        activeFault.set(fault);
        return fault;
    }

    public void apply() {
        FaultResponse fault = activeFault.get();
        if (fault == null) {
            return;
        }
        if (!fault.expiresAt().isAfter(Instant.now())) {
            activeFault.compareAndSet(fault, null);
            return;
        }

        if (fault.type() == FaultType.LATENCY) {
            try {
                Thread.sleep(fault.magnitude());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Request interrupted", exception);
            }
        } else if (fault.type() == FaultType.ERROR) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Injected service error");
        } else {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service unavailable due to injected crash");
        }
    }
}
