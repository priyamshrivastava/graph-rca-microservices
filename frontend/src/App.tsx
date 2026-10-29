import { lazy, Suspense, useEffect } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { Sidebar } from "./components/Sidebar";
import { Topbar } from "./components/Topbar";
import { useLiveStream } from "./hooks/useLiveStream";
import { useUiStore } from "./store/telemetry";

const OverviewPage = lazy(async () => ({ default: (await import("./pages/OverviewPage")).OverviewPage }));
const GraphPage = lazy(async () => ({ default: (await import("./pages/GraphPage")).GraphPage }));
const ComingSoonPage = lazy(async () => ({ default: (await import("./pages/ComingSoonPage")).ComingSoonPage }));

function PageLoading() {
  return <div className="page-content"><div className="metric-grid">{Array.from({ length: 4 }, (_, index) => <div className="skeleton metric-skeleton" key={index} />)}</div><div className="skeleton graph-skeleton" /></div>;
}

export default function App() {
  useLiveStream();
  const location = useLocation();
  const lightTheme = useUiStore((state) => state.lightTheme);

  useEffect(() => {
    document.documentElement.dataset.theme = lightTheme ? "light" : "dark";
  }, [lightTheme]);

  return (
    <div className={`app-shell ${lightTheme ? "theme-light" : ""}`}>
      <Sidebar />
      <main className="main-shell">
        <Topbar />
        <AnimatePresence mode="wait">
          <motion.div key={location.pathname} className="route-content" initial={{ opacity: 0, y: 5 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -3 }} transition={{ duration: 0.18 }}>
            <Suspense fallback={<PageLoading />}>
              <Routes location={location}>
                <Route path="/" element={<OverviewPage />} />
                <Route path="/graph" element={<GraphPage />} />
                <Route path="/incidents" element={<ComingSoonPage />} />
                <Route path="/faults" element={<ComingSoonPage />} />
                <Route path="/metrics" element={<ComingSoonPage />} />
                <Route path="/evaluation" element={<ComingSoonPage />} />
                <Route path="/settings" element={<ComingSoonPage />} />
                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
            </Suspense>
          </motion.div>
        </AnimatePresence>
      </main>
    </div>
  );
}
