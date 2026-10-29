import type { EdgeMetrics, GraphSnapshot, ServiceMetrics } from "../types";

const apiBase = (import.meta.env.VITE_API_BASE_URL ?? "/api").replace(/\/$/, "");

async function request<T>(path: string, signal?: AbortSignal): Promise<T> {
  const response = await fetch(`${apiBase}${path}`, {
    headers: { Accept: "application/json" },
    signal,
  });
  if (!response.ok) {
    const body = await response.text();
    throw new Error(body || `API request failed (${response.status})`);
  }
  return response.json() as Promise<T>;
}

export const api = {
  graph: (signal?: AbortSignal) => request<GraphSnapshot>("/graph/current", signal),
  graphHistory: (limit = 50, signal?: AbortSignal) =>
    request<GraphSnapshot[]>(`/graph/history?limit=${limit}`, signal),
  edges: (signal?: AbortSignal) => request<EdgeMetrics[]>("/metrics/edges", signal),
  services: (signal?: AbortSignal) => request<ServiceMetrics[]>("/services", signal),
  streamUrl: `${apiBase}/stream`,
};
