import { motion } from "framer-motion";
import { Activity, ArrowDownRight, ArrowUpRight, Clock3, Gauge, Radio, Server, ShieldCheck } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { api } from "../api/client";
import { Panel } from "../components/Panel";
import { StatusBadge } from "../components/StatusBadge";
import { useTelemetryStore } from "../store/telemetry";
import type { EdgeMetrics, GraphSnapshot, ServiceMetrics } from "../types";

function useOverviewData() {
  return useQuery({ queryKey: ["graph"], queryFn: ({ signal }) => api.graph(signal), refetchInterval: 5000 });
}

function buildLatencyHistory(history: GraphSnapshot[], liveGraph: GraphSnapshot | null) {
  const snapshots = [...history];
  if (liveGraph && !snapshots.some((snapshot) => snapshot.capturedAt === liveGraph.capturedAt)) {
    snapshots.push(liveGraph);
  }
  return snapshots
    .sort((left, right) => left.capturedAt.localeCompare(right.capturedAt))
    .map((snapshot) => {
      const edges = snapshot.edges;
      const latency = edges.length ? edges.reduce((total, edge) => total + edge.meanLatencyMs, 0) / edges.length : 0;
      return {
        time: new Date(snapshot.capturedAt).toLocaleTimeString([], { minute: "2-digit", second: "2-digit" }),
        latency: Math.round(latency),
      };
    });
}

function MetricCard({ label, value, unit, icon: Icon, tone, delta }: {
  label: string;
  value: string;
  unit?: string;
  icon: typeof Server;
  tone: string;
  delta: string;
}) {
  return (
    <motion.div className="metric-card panel" initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.35 }}>
      <div className="metric-card-top"><span className="metric-label">{label}</span><span className={`metric-icon ${tone}`}><Icon size={18} /></span></div>
      <div className="metric-value">{value}<small>{unit}</small></div>
      <div className="metric-foot"><span className={`metric-delta ${tone}`}>{delta}</span><span>across latest window</span></div>
    </motion.div>
  );
}

function LatencyChart({ history, liveGraph }: { history: GraphSnapshot[]; liveGraph: GraphSnapshot | null }) {
  const data = buildLatencyHistory(history, liveGraph);
  return (
    <div className="chart-wrap">
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={data} margin={{ top: 10, right: 8, left: -18, bottom: 0 }}>
          <defs><linearGradient id="latency-fill" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="#52e2b2" stopOpacity={0.28} /><stop offset="100%" stopColor="#52e2b2" stopOpacity={0} /></linearGradient></defs>
          <CartesianGrid stroke="rgba(148,163,184,.09)" vertical={false} />
          <XAxis dataKey="time" tickLine={false} axisLine={false} tick={{ fill: "#758199", fontSize: 10 }} minTickGap={30} />
          <YAxis tickLine={false} axisLine={false} tick={{ fill: "#758199", fontSize: 10 }} />
          <Tooltip contentStyle={{ background: "#111827", border: "1px solid rgba(148,163,184,.2)", borderRadius: 10, color: "#e5edf8" }} />
          <Area type="monotone" dataKey="latency" name="Avg. latency (ms)" stroke="#52e2b2" strokeWidth={2} fill="url(#latency-fill)" isAnimationActive={false} />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}

function ActivityFeed({ edges }: { edges: EdgeMetrics[] }) {
  const recent = [...edges].sort((a, b) => (b.lastSeen ?? "").localeCompare(a.lastSeen ?? "")).slice(0, 5);
  if (!recent.length) return <div className="empty-state"><Radio size={20} /><strong>Waiting for trace traffic</strong><span>Call UserService to start the live feed.</span></div>;
  return (
    <div className="activity-list">
      {recent.map((edge, index) => (
        <div className="activity-row" key={edge.id}>
          <span className={`activity-dot ${edge.health === "UNHEALTHY" ? "dot-red" : "dot-green"}`} />
          <div className="activity-copy"><strong>{edge.caller} <span>→</span> {edge.callee}</strong><small>{edge.health === "UNHEALTHY" ? "Edge health degraded" : "Trace observed"} · {edge.lastSeen ? new Date(edge.lastSeen).toLocaleTimeString() : "just now"}</small></div>
          <span className="activity-latency">{Math.round(edge.meanLatencyMs)}<small>ms</small></span>
          {index === 0 && <span className="activity-live">LIVE</span>}
        </div>
      ))}
    </div>
  );
}

function ServiceHealthList({ services }: { services: ServiceMetrics[] }) {
  return (
    <div className="service-list">
      {services.map((service, index) => (
        <motion.div className="service-row" key={service.id} initial={{ opacity: 0, x: -8 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: index * 0.05 }}>
          <div className="service-monogram">{service.name.slice(0, 1)}</div>
          <div className="service-row-info"><strong>{service.name}</strong><small>{service.calls} calls · {service.callsPerSecond.toFixed(2)} req/s</small></div>
          <StatusBadge status={service.health} />
        </motion.div>
      ))}
    </div>
  );
}

export function OverviewPage() {
  const query = useOverviewData();
  const historyQuery = useQuery({ queryKey: ["graph-history"], queryFn: ({ signal }) => api.graphHistory(18, signal), refetchInterval: 10000 });
  const liveGraph = useTelemetryStore((state) => state.graph);
  const graph: GraphSnapshot | undefined = liveGraph ?? query.data;
  const services = graph?.nodes ?? [];
  const edges = graph?.edges ?? [];
  const healthy = services.filter((service) => service.health === "HEALTHY").length;
  const unhealthy = services.filter((service) => service.health === "UNHEALTHY").length;
  const callsPerSecond = edges.reduce((total, edge) => total + edge.callsPerSecond, 0);
  const avgLatency = edges.length ? edges.reduce((total, edge) => total + edge.meanLatencyMs, 0) / edges.length : 0;
  const errorRate = edges.length ? edges.reduce((total, edge) => total + edge.errorRate, 0) / edges.length : 0;

  if (query.isError && !graph) return <div className="error-state"><ShieldCheck size={26} /><h2>Couldn’t load telemetry</h2><p>{query.error.message}</p><button className="button-primary" onClick={() => void query.refetch()}>Retry connection</button></div>;

  return (
    <div className="page-content">
      {query.isLoading && !graph ? <div className="metric-grid">{Array.from({ length: 4 }, (_, i) => <div className="skeleton metric-skeleton" key={i} />)}</div> : (
        <div className="metric-grid">
          <MetricCard label="Healthy services" value={`${healthy}`} unit={`/ ${services.length}`} icon={Server} tone="tone-green" delta={`${unhealthy} degraded`} />
          <MetricCard label="Calls per second" value={callsPerSecond.toFixed(1)} icon={Activity} tone="tone-blue" delta="Live throughput" />
          <MetricCard label="Average latency" value={Math.round(avgLatency).toString()} unit="ms" icon={Clock3} tone="tone-violet" delta={edges.length ? "Across active edges" : "Awaiting traffic"} />
          <MetricCard label="Error rate" value={`${(errorRate * 100).toFixed(2)}`} unit="%" icon={Gauge} tone={errorRate > 0.05 ? "tone-red" : "tone-green"} delta={errorRate > 0.05 ? "Above normal" : "Within healthy range"} />
        </div>
      )}

      <div className="overview-grid">
        <Panel title="Latency trend" eyebrow="LIVE TELEMETRY" action={<span className="chart-legend"><i />Average latency</span>} className="latency-panel">
          {historyQuery.data?.length || liveGraph ? <LatencyChart history={historyQuery.data ?? []} liveGraph={liveGraph} /> : <div className="chart-empty"><Radio size={21} /><strong>Collecting latency snapshots</strong><span>Traffic history appears here as the graph is sampled.</span></div>}
          <div className="chart-summary"><span><i className="summary-icon"><ArrowDownRight size={14} /></i>Rolling edge baseline</span><strong>{Math.round(avgLatency)} <small>ms</small></strong></div>
        </Panel>
        <Panel title="Service health" eyebrow="FLEET STATUS" action={<span className="small-count">{services.length} services</span>}>
          {services.length ? <ServiceHealthList services={services} /> : <div className="empty-state"><Server size={20} /><strong>Services are standing by</strong><span>Send traffic to populate service health.</span></div>}
        </Panel>
      </div>

      <Panel title="Recent activity" eyebrow="TRACE FEED" action={<span className="live-tag"><i /> LIVE</span>}>
        <ActivityFeed edges={edges} />
      </Panel>
      <div className="overview-footnote"><ArrowUpRight size={13} /> Live service statistics update as trace events arrive.</div>
    </div>
  );
}
