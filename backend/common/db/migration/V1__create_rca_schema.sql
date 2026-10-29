CREATE TABLE trace_events (
    id BIGSERIAL PRIMARY KEY,
    trace_id VARCHAR(64) NOT NULL,
    caller VARCHAR(100) NOT NULL,
    callee VARCHAR(100) NOT NULL,
    event_timestamp TIMESTAMPTZ NOT NULL,
    latency_ms DOUBLE PRECISION NOT NULL CHECK (latency_ms >= 0),
    status VARCHAR(16) NOT NULL CHECK (status IN ('SUCCESS', 'ERROR'))
);

CREATE INDEX idx_trace_events_timestamp ON trace_events (event_timestamp DESC);
CREATE INDEX idx_trace_events_edge_timestamp ON trace_events (caller, callee, event_timestamp DESC);

CREATE TABLE incidents (
    id UUID PRIMARY KEY,
    detected_at TIMESTAMPTZ NOT NULL,
    root_cause_service VARCHAR(100) NOT NULL,
    confidence DOUBLE PRECISION NOT NULL CHECK (confidence BETWEEN 0 AND 1),
    summary TEXT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'OPEN'
);

CREATE INDEX idx_incidents_detected_at ON incidents (detected_at DESC);

CREATE TABLE graph_snapshots (
    id BIGSERIAL PRIMARY KEY,
    captured_at TIMESTAMPTZ NOT NULL,
    graph JSONB NOT NULL
);

CREATE INDEX idx_graph_snapshots_captured_at ON graph_snapshots (captured_at DESC);
