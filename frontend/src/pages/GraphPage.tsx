import { useMemo, useState } from "react";
import { motion } from "framer-motion";
import { Activity, ArrowRight, Clock3, X } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import {
  Background, Controls, Handle, MiniMap, Position, ReactFlow, type Edge, type Node, type NodeProps,
} from "@xyflow/react";
import "@xyflow/react/dist/style.css";
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { api } from "../api/client";
import { Panel } from "../components/Panel";
import { StatusBadge } from "../components/StatusBadge";
import { useTelemetryStore } from "../store/telemetry";
import type { EdgeMetrics, GraphSnapshot, ServiceMetrics } from "../types";

const nodePositions: Record<string, { x: number; y: number }> = {
  UserService: { x: 60, y: 210 },
  OrderService: { x: 330, y: 210 },
  PaymentService: { x: 600, y: 210 },
  InventoryService: { x: 870, y: 210 },
  NotificationService: { x: 1140, y: 210 },
};

function GraphNode({ data }: NodeProps) {
  const service = data.service as ServiceMetrics;
  const anomalous = service.health === "UNHEALTHY";
  return (
    <>
      <Handle type="target" position={Position.Left} className="flow-handle" />
      <div className={`flow-node ${anomalous ? "flow-node-unhealthy" : ""} ${service.health === "HEALTHY" ? "flow-node-healthy" : ""}`}>
        <span className="flow-node-icon"><Activity size={17} /></span>
        <div className="flow-node-copy"><strong>{service.name}</strong><small>{service.health === "UNKNOWN" ? "Awaiting telemetry" : `${service.callsPerSecond.toFixed(2)} req/s`}</small></div>
        <i className={`flow-node-status ${service.health.toLowerCase()}`} />
      </div>
      <Handle type="source" position={Position.Right} className="flow-handle" />
    </>
  );
}

function EdgeDetail({ edge, onClose }: { edge: EdgeMetrics; onClose: () => void }) {
  const chartData = [
    { name: "Baseline mean", latency: edge.meanLatencyMs },
    { name: "EWMA", latency: edge.ewmaLatencyMs },
    { name: "Current p95", latency: edge.p95LatencyMs },
  ];
  return (
    <motion.aside className="edge-detail panel" initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: 20 }}>
      <div className="detail-header"><div><span className="eyebrow">EDGE TELEMETRY</span><h2>{edge.caller}</h2></div><button className="icon-button" onClick={onClose} aria-label="Close details"><X size={17} /></button></div>
      <div className="detail-route"><span>{edge.caller}</span><ArrowRight size={15} /><span>{edge.callee}</span></div>
      <StatusBadge status={edge.health} />
      <div className="detail-stats">
        <div><small>Mean latency</small><strong>{Math.round(edge.meanLatencyMs)} <i>ms</i></strong></div>
        <div><small>P95 latency</small><strong>{Math.round(edge.p95LatencyMs)} <i>ms</i></strong></div>
        <div><small>Calls / sec</small><strong>{edge.callsPerSecond.toFixed(2)}</strong></div>
        <div><small>Error rate</small><strong>{(edge.errorRate * 100).toFixed(2)}<i>%</i></strong></div>
        <div><small>Z-score</small><strong>{Number.isFinite(edge.zScore) ? edge.zScore.toFixed(2) : "—"}</strong></div>
        <div><small>Total calls</small><strong>{edge.callCount.toLocaleString()}</strong></div>
      </div>
      <div className="detail-chart-title"><span>Baseline vs current</span><Clock3 size={14} /></div>
      <div className="detail-chart">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={chartData}><defs><linearGradient id="detail-fill" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="#52e2b2" stopOpacity={0.28} /><stop offset="100%" stopColor="#52e2b2" stopOpacity={0} /></linearGradient></defs>
            <CartesianGrid stroke="rgba(148,163,184,.09)" vertical={false} /><XAxis dataKey="name" tickLine={false} axisLine={false} tick={{ fill: "#758199", fontSize: 9 }} /><YAxis tickLine={false} axisLine={false} tick={{ fill: "#758199", fontSize: 9 }} width={35} />
            <Tooltip contentStyle={{ background: "#111827", border: "1px solid rgba(148,163,184,.2)", borderRadius: 10, color: "#e5edf8" }} /><Area dataKey="latency" stroke="#52e2b2" fill="url(#detail-fill)" />
          </AreaChart>
        </ResponsiveContainer>
      </div>
      <div className="detail-note">Rolling baseline uses the configured trailing sample window.</div>
    </motion.aside>
  );
}

function ServiceDetail({ service, edges, onClose }: { service: ServiceMetrics; edges: EdgeMetrics[]; onClose: () => void }) {
  const related = edges.find((edge) => edge.caller === service.id) ?? edges.find((edge) => edge.callee === service.id);
  const chartData = related ? [
    { name: "Baseline mean", latency: related.meanLatencyMs },
    { name: "EWMA", latency: related.ewmaLatencyMs },
    { name: "Current p95", latency: related.p95LatencyMs },
  ] : [];
  return (
    <motion.aside className="edge-detail panel" initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: 20 }}>
      <div className="detail-header"><div><span className="eyebrow">SERVICE TELEMETRY</span><h2>{service.name}</h2></div><button className="icon-button" onClick={onClose} aria-label="Close details"><X size={17} /></button></div>
      <div className="detail-service-status"><StatusBadge status={service.health} /></div>
      <div className="detail-stats">
        <div><small>Calls / sec</small><strong>{service.callsPerSecond.toFixed(2)}</strong></div>
        <div><small>Total calls</small><strong>{service.calls.toLocaleString()}</strong></div>
        <div><small>Average latency</small><strong>{Math.round(service.averageLatencyMs)} <i>ms</i></strong></div>
        <div><small>Error rate</small><strong>{(service.errorRate * 100).toFixed(2)}<i>%</i></strong></div>
      </div>
      <div className="detail-chart-title"><span>Baseline vs current{related ? ` · ${related.caller} → ${related.callee}` : ""}</span><Clock3 size={14} /></div>
      {related ? <div className="detail-chart">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={chartData}><defs><linearGradient id="service-detail-fill" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="#52e2b2" stopOpacity={0.28} /><stop offset="100%" stopColor="#52e2b2" stopOpacity={0} /></linearGradient></defs>
            <CartesianGrid stroke="rgba(148,163,184,.09)" vertical={false} /><XAxis dataKey="name" tickLine={false} axisLine={false} tick={{ fill: "#758199", fontSize: 9 }} /><YAxis tickLine={false} axisLine={false} tick={{ fill: "#758199", fontSize: 9 }} width={35} />
            <Tooltip contentStyle={{ background: "#111827", border: "1px solid rgba(148,163,184,.2)", borderRadius: 10, color: "#e5edf8" }} /><Area dataKey="latency" stroke="#52e2b2" fill="url(#service-detail-fill)" />
          </AreaChart>
        </ResponsiveContainer>
      </div> : <div className="detail-chart-empty">No edge samples available for this service yet.</div>}
      {service.lastSeen && <div className="detail-note">Last trace observed {new Date(service.lastSeen).toLocaleString()}.</div>}
    </motion.aside>
  );
}

export function GraphPage() {
  const query = useQuery({ queryKey: ["graph"], queryFn: ({ signal }) => api.graph(signal), refetchInterval: 5000 });
  const liveGraph = useTelemetryStore((state) => state.graph);
  const graph: GraphSnapshot | undefined = liveGraph ?? query.data;
  const [selectedServiceId, setSelectedServiceId] = useState<string | null>(null);
  const [selectedEdge, setSelectedEdge] = useState<EdgeMetrics | null>(null);
  const nodeTypes = useMemo(() => ({ serviceNode: GraphNode }), []);
  const nodes: Node[] = (graph?.nodes ?? []).map((service) => ({
    id: service.id,
    type: "serviceNode",
    position: nodePositions[service.id] ?? { x: 100 + (graph?.nodes.findIndex((entry) => entry.id === service.id) ?? 0) * 270, y: 210 },
    data: { service },
  }));
  const edges: Edge[] = (graph?.edges ?? []).map((edge) => ({
    id: edge.id,
    source: edge.caller,
    target: edge.callee,
    animated: true,
    style: {
      stroke: edge.health === "UNHEALTHY" ? "#fb7185" : edge.callsPerSecond > 0 ? "#52e2b2" : "#596579",
      strokeWidth: Math.max(1.6, Math.min(5, 1.5 + edge.callsPerSecond * 1.5)),
    },
    label: `${Math.round(edge.meanLatencyMs)} ms`,
    labelStyle: { fill: "#9ba8ba", fontSize: 10 },
    labelBgStyle: { fill: "#111827", fillOpacity: 0.92 },
    labelBgPadding: [5, 3],
  }));
  const selectedService = graph?.nodes.find((service) => service.id === selectedServiceId);
  if (query.isError && !graph) return <div className="error-state"><Activity size={26} /><h2>Graph unavailable</h2><p>{query.error.message}</p><button className="button-primary" onClick={() => void query.refetch()}>Retry connection</button></div>;

  return (
    <div className="page-content graph-page">
      <div className="graph-toolbar">
        <div className="graph-legend"><span><i className="legend-green" />Healthy edge</span><span><i className="legend-amber" />Awaiting data</span><span><i className="legend-red" />Degraded edge</span></div>
        <div className="graph-live"><i /> Updates every second</div>
      </div>
      <Panel title="Service dependencies" eyebrow="LIVE TOPOLOGY" action={<span className="small-count">{nodes.length} nodes · {edges.length} edges</span>} className="graph-panel">
        {query.isLoading && !graph ? <div className="graph-loading"><div className="skeleton graph-skeleton" /></div> : nodes.length ? (
          <div className="flow-canvas">
            <ReactFlow nodes={nodes} edges={edges} nodeTypes={nodeTypes} fitView fitViewOptions={{ padding: 0.2, maxZoom: 1.2 }} minZoom={0.4} maxZoom={1.5} proOptions={{ hideAttribution: true }}
              onNodeClick={(_, node) => {
                setSelectedEdge(null);
                setSelectedServiceId(node.id);
              }}
              onEdgeClick={(_, edge) => {
                setSelectedServiceId(null);
                setSelectedEdge(graph?.edges.find((item) => item.id === edge.id) ?? null);
              }}>
              <Background color="rgba(148,163,184,.17)" gap={24} size={1} /><MiniMap nodeColor={(node) => ((node.data.service as ServiceMetrics).health === "UNHEALTHY" ? "#fb7185" : "#52e2b2")} maskColor="rgba(9,13,22,.7)" /><Controls />
            </ReactFlow>
          </div>
        ) : <div className="chart-empty"><Activity size={22} /><strong>No dependencies discovered yet</strong><span>Generate service traffic to populate the graph.</span></div>}
      </Panel>
      {selectedEdge && <EdgeDetail edge={selectedEdge} onClose={() => setSelectedEdge(null)} />}
      {selectedService && !selectedEdge && <ServiceDetail service={selectedService} edges={graph?.edges ?? []} onClose={() => setSelectedServiceId(null)} />}
      <div className="graph-footnote">Click a node or connection to inspect its latest edge metrics. Use the controls to zoom and pan.</div>
    </div>
  );
}
