package smo;

public record SimulationEvent(double time, EventType type, int sourceId, int workerId, long order) {
}
