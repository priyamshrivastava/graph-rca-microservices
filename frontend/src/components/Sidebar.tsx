import {
  Activity, ChevronLeft, ChevronRight, CircleGauge, FlaskConical, GitBranch,
  History, Settings2, ShieldAlert, X,
} from "lucide-react";
import { NavLink } from "react-router-dom";
import { useUiStore } from "../store/telemetry";

const navigation = [
  { to: "/", label: "Overview", icon: CircleGauge },
  { to: "/graph", label: "Service graph", icon: GitBranch },
  { to: "/incidents", label: "Incidents", icon: ShieldAlert, badge: "M3" },
  { to: "/faults", label: "Fault injection", icon: FlaskConical, badge: "M3" },
  { to: "/metrics", label: "Metrics", icon: Activity, badge: "M4" },
  { to: "/evaluation", label: "Evaluation", icon: History, badge: "M4" },
  { to: "/settings", label: "Settings", icon: Settings2, badge: "M4" },
];

export function Sidebar() {
  const collapsed = useUiStore((state) => state.sidebarCollapsed);
  const mobileOpen = useUiStore((state) => state.mobileMenuOpen);
  const toggleSidebar = useUiStore((state) => state.toggleSidebar);
  const setMobileMenuOpen = useUiStore((state) => state.setMobileMenuOpen);

  return (
    <>
      {mobileOpen && <button className="mobile-backdrop" onClick={() => setMobileMenuOpen(false)} aria-label="Close navigation" />}
      <aside className={`sidebar ${collapsed ? "sidebar-collapsed" : ""} ${mobileOpen ? "sidebar-mobile-open" : ""}`}>
        <div className="brand">
          <div className="brand-mark"><Activity size={19} strokeWidth={2.5} /></div>
          {!collapsed && <div className="brand-copy"><strong>graph<span>rca</span></strong><small>OBSERVABILITY PLATFORM</small></div>}
          <button className="icon-button mobile-close" onClick={() => setMobileMenuOpen(false)} aria-label="Close navigation"><X size={17} /></button>
        </div>
        <div className="workspace-label">{!collapsed && "WORKSPACE"}</div>
        <nav className="nav-list" aria-label="Main navigation">
          {navigation.map(({ to, label, icon: Icon, badge }) => (
            <NavLink key={to} to={to} end={to === "/"} className={({ isActive }) => `nav-link ${isActive ? "nav-link-active" : ""}`} onClick={() => setMobileMenuOpen(false)} title={collapsed ? label : undefined}>
              <Icon size={18} />
              {!collapsed && <><span>{label}</span>{badge && <span className="nav-badge">{badge}</span>}</>}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <div className="engine-card">
            <span className="engine-orb" />
            {!collapsed && <div><strong>RCA engine</strong><small>Training-free · live</small></div>}
          </div>
          <button className="collapse-button" onClick={toggleSidebar} aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}>
            {collapsed ? <ChevronRight size={17} /> : <><ChevronLeft size={17} /><span>Collapse sidebar</span></>}
          </button>
        </div>
      </aside>
    </>
  );
}
