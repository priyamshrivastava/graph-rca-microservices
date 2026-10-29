import { Menu, Moon, Sun } from "lucide-react";
import { useLocation } from "react-router-dom";
import { useTelemetryStore, useUiStore } from "../store/telemetry";

const titles: Record<string, { title: string; subtitle: string }> = {
  "/": { title: "Overview", subtitle: "Your system at a glance" },
  "/graph": { title: "Live service graph", subtitle: "Explore dependencies and edge health" },
};

export function Topbar() {
  const location = useLocation();
  const connection = useTelemetryStore((state) => state.connection);
  const toggleTheme = useUiStore((state) => state.toggleTheme);
  const lightTheme = useUiStore((state) => state.lightTheme);
  const setMobileMenuOpen = useUiStore((state) => state.setMobileMenuOpen);
  const page = titles[location.pathname] ?? { title: "Workspace", subtitle: "Graph RCA mission control" };

  return (
    <header className="topbar">
      <button className="icon-button mobile-menu-button" onClick={() => setMobileMenuOpen(true)} aria-label="Open navigation"><Menu size={19} /></button>
      <div className="page-heading"><h1>{page.title}</h1><p>{page.subtitle}</p></div>
      <div className="topbar-actions">
        <span className={`connection-pill connection-${connection}`}><i />{connection === "connected" ? "Live connection" : "Reconnecting"}</span>
        <button className="icon-button theme-toggle" onClick={toggleTheme} title={`Switch to ${lightTheme ? "dark" : "light"} theme`} aria-label="Toggle theme">
          {lightTheme ? <Moon size={17} /> : <Sun size={17} />}
        </button>
        <div className="avatar">PR</div>
      </div>
    </header>
  );
}
