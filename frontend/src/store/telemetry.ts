import { create } from "zustand";
import type { GraphSnapshot } from "../types";

type ConnectionState = "connected" | "reconnecting";

interface TelemetryState {
  graph: GraphSnapshot | null;
  connection: ConnectionState;
  setGraph: (graph: GraphSnapshot) => void;
  setConnection: (connection: ConnectionState) => void;
}

export const useTelemetryStore = create<TelemetryState>((set) => ({
  graph: null,
  connection: "reconnecting",
  setGraph: (graph) => set({ graph }),
  setConnection: (connection) => set({ connection }),
}));

interface UiState {
  sidebarCollapsed: boolean;
  mobileMenuOpen: boolean;
  lightTheme: boolean;
  toggleSidebar: () => void;
  setMobileMenuOpen: (open: boolean) => void;
  toggleTheme: () => void;
}

export const useUiStore = create<UiState>((set) => ({
  sidebarCollapsed: false,
  mobileMenuOpen: false,
  lightTheme: false,
  toggleSidebar: () => set((state) => ({ sidebarCollapsed: !state.sidebarCollapsed })),
  setMobileMenuOpen: (mobileMenuOpen) => set({ mobileMenuOpen }),
  toggleTheme: () => set((state) => ({ lightTheme: !state.lightTheme })),
}));
