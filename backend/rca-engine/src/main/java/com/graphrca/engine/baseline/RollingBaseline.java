package com.graphrca.engine.baseline;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

public class RollingBaseline {
    private final int windowSize;
    private final double ewmaAlpha;
    private final Deque<Double> latencies = new ArrayDeque<>();
    private final Deque<Double> errors = new ArrayDeque<>();
    private double meanLatency;
    private double standardDeviation;
    private double ewmaLatency;
    private double errorRate;
    private double ewmaErrorRate;
    private boolean initialized;

    public RollingBaseline(int windowSize, double ewmaAlpha) {
        if (windowSize < 1) {
            throw new IllegalArgumentException("windowSize must be greater than zero");
        }
        if (!(ewmaAlpha > 0 && ewmaAlpha <= 1)) {
            throw new IllegalArgumentException("ewmaAlpha must be in (0, 1]");
        }
        this.windowSize = windowSize;
        this.ewmaAlpha = ewmaAlpha;
    }

    public synchronized void add(double latencyMs, boolean error) {
        if (!Double.isFinite(latencyMs) || latencyMs < 0) {
            throw new IllegalArgumentException("latencyMs must be a finite, non-negative value");
        }
        if (latencies.size() == windowSize) {
            latencies.removeFirst();
            errors.removeFirst();
        }
        latencies.addLast(latencyMs);
        errors.addLast(error ? 1.0 : 0.0);
        updateStatistics();
    }

    private void updateStatistics() {
        meanLatency = latencies.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = latencies.stream().mapToDouble(value -> Math.pow(value - meanLatency, 2)).average().orElse(0);
        standardDeviation = Math.sqrt(variance);
        errorRate = errors.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        double currentLatency = latencies.getLast();
        double currentError = errors.getLast();
        if (!initialized) {
            ewmaLatency = currentLatency;
            ewmaErrorRate = currentError;
            initialized = true;
        } else {
            ewmaLatency = ewmaAlpha * currentLatency + (1 - ewmaAlpha) * ewmaLatency;
            ewmaErrorRate = ewmaAlpha * currentError + (1 - ewmaAlpha) * ewmaErrorRate;
        }
    }

    public synchronized double meanLatencyMs() {
        return meanLatency;
    }

    public synchronized double standardDeviationMs() {
        return standardDeviation;
    }

    public synchronized double ewmaLatencyMs() {
        return ewmaLatency;
    }

    public synchronized double errorRate() {
        return errorRate;
    }

    public synchronized double ewmaErrorRate() {
        return ewmaErrorRate;
    }

    public synchronized double zScore(double latencyMs) {
        if (standardDeviation == 0) {
            return latencyMs == meanLatency ? 0 : (latencyMs > meanLatency ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
        }
        return (latencyMs - meanLatency) / standardDeviation;
    }

    public synchronized double p95LatencyMs() {
        if (latencies.isEmpty()) {
            return 0;
        }
        List<Double> sorted = new ArrayList<>(latencies);
        Collections.sort(sorted);
        int index = Math.max(0, (int) Math.ceil(sorted.size() * 0.95) - 1);
        return sorted.get(index);
    }

    public synchronized int sampleCount() {
        return latencies.size();
    }
}
