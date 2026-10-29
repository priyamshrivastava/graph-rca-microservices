import { motion } from "framer-motion";
import { Activity, ArrowUpRight, Construction } from "lucide-react";
import { useLocation } from "react-router-dom";

const content: Record<string, { title: string; phase: string; description: string; icon: typeof Activity }> = {
  "/incidents": { title: "Incident workspace", phase: "M3", description: "Ranked root-cause candidates, evidence and propagation timelines are on the roadmap for the next milestone.", icon: Activity },
  "/faults": { title: "Fault injection lab", phase: "M3", description: "Trigger controlled service faults and follow their impact across the dependency graph.", icon: Construction },
  "/metrics": { title: "Metrics & baselines", phase: "M4", description: "Explore edge latency distributions, error rates and statistical baseline behaviour.", icon: Activity },
  "/evaluation": { title: "Evaluation suite", phase: "M4", description: "Compare detection quality, time-to-detect and false-positive rates across experiments.", icon: ArrowUpRight },
  "/settings": { title: "Workspace settings", phase: "M4", description: "Configure detection thresholds, baseline windows and console preferences.", icon: Construction },
};

export function ComingSoonPage() {
  const location = useLocation();
  const page = content[location.pathname] ?? content["/incidents"];
  const Icon = page.icon;
  return (
    <div className="coming-page">
      <motion.div className="coming-card panel" initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}>
        <div className="coming-icon"><Icon size={25} /></div>
        <span className="eyebrow">{page.phase} ROADMAP</span>
        <h2>{page.title}</h2>
        <p>{page.description}</p>
        <div className="coming-progress"><span><i /></span><small>In active development</small></div>
      </motion.div>
    </div>
  );
}
