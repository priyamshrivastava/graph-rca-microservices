import { Circle } from "lucide-react";
import type { ServiceHealth } from "../types";

const labels: Record<ServiceHealth, string> = {
  HEALTHY: "Healthy",
  UNHEALTHY: "Degraded",
  UNKNOWN: "Awaiting traffic",
};

export function StatusBadge({ status }: { status: ServiceHealth }) {
  return (
    <span className={`status-badge status-${status.toLowerCase()}`}>
      <Circle size={7} fill="currentColor" />
      {labels[status]}
    </span>
  );
}
