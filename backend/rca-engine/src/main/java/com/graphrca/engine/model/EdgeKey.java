package com.graphrca.engine.model;

public record EdgeKey(String caller, String callee) {
    @Override
    public String toString() {
        return caller + "->" + callee;
    }
}
