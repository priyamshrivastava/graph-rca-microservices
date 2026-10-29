export type ServiceHealth = "HEALTHY" | "UNHEALTHY" | "UNKNOWN";

export interface ServiceMetrics {
  id: string;
  name: string;
  health: ServiceHealth;
  calls: number;
  callsPerSecond: number;
  averageLatencyMs: number;
  errorRate: number;
  lastSeen: string | null;
}

export interface EdgeMetrics {
  id: string;
  caller: string;
  callee: string;
  callCount: number;
  callsPerSecond: number;
  meanLatencyMs: number;
  standardDeviationMs: number;
  ewmaLatencyMs: number;
  p95LatencyMs: number;
  errorRate: number;
  ewmaErrorRate: number;
  zScore: number;
  lastSeen: string | null;
  health: ServiceHealth;
}

export interface GraphSnapshot {
  capturedAt: string;
  nodes: ServiceMetrics[];
  edges: EdgeMetrics[];
}
