import { useEffect } from "react";
import { api } from "../api/client";
import { useTelemetryStore } from "../store/telemetry";
import type { GraphSnapshot } from "../types";

export function useLiveStream() {
  const setGraph = useTelemetryStore((state) => state.setGraph);
  const setConnection = useTelemetryStore((state) => state.setConnection);

  useEffect(() => {
    const source = new EventSource(api.streamUrl);
    const handleGraph = (event: Event) => {
      try {
        setGraph(JSON.parse((event as MessageEvent<string>).data) as GraphSnapshot);
      } catch {
        setConnection("reconnecting");
      }
    };
    source.addEventListener("open", () => setConnection("connected"));
    source.addEventListener("graph", handleGraph);
    source.addEventListener("error", () => setConnection("reconnecting"));
    return () => source.close();
  }, [setConnection, setGraph]);
}
