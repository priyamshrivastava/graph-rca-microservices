package com.graphrca.engine.baseline;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RollingBaselineTest {
    @Test
    void computesMeanDeviationAndPercentileFromTheWindow() {
        RollingBaseline baseline = new RollingBaseline(3, 0.5);
        baseline.add(10, false);
        baseline.add(20, true);

        assertEquals(15, baseline.meanLatencyMs());
        assertEquals(5, baseline.standardDeviationMs());
        assertEquals(20, baseline.p95LatencyMs());
        assertEquals(0.5, baseline.errorRate());
        assertEquals(15, baseline.ewmaLatencyMs());
        assertEquals(0.5, baseline.ewmaErrorRate());
    }

    @Test
    void evictsOldestObservationWhenWindowIsFull() {
        RollingBaseline baseline = new RollingBaseline(2, 0.5);
        baseline.add(10, true);
        baseline.add(20, false);
        baseline.add(30, false);

        assertEquals(2, baseline.sampleCount());
        assertEquals(25, baseline.meanLatencyMs());
        assertEquals(0, baseline.errorRate());
        assertEquals(30, baseline.p95LatencyMs());
    }

    @Test
    void rejectsInvalidWindowAndSamples() {
        assertThrows(IllegalArgumentException.class, () -> new RollingBaseline(0, 0.5));
        RollingBaseline baseline = new RollingBaseline(2, 0.2);
        assertThrows(IllegalArgumentException.class, () -> baseline.add(-1, false));
    }
}
