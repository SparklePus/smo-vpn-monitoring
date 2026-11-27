package smo;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public final class EventCalendar {
    private static final Comparator<SimulationEvent> ORDER =
            Comparator.comparingDouble(SimulationEvent::time)
                    .thenComparingInt(e -> e.type() == EventType.SERVICE_COMPLETION ? 0 : 1)
                    .thenComparingLong(SimulationEvent::order);
    private final PriorityQueue<SimulationEvent> events = new PriorityQueue<>(ORDER);
    private long nextOrder;

    public void schedule(double time, EventType type, int sourceId, int workerId) {
        if (!Double.isFinite(time) || time < 0) {
            throw new IllegalArgumentException("Время события должно быть конечным и неотрицательным");
        }
        events.add(new SimulationEvent(time, type, sourceId, workerId, nextOrder++));
    }

    public SimulationEvent pollNext() { return events.poll(); }
    public SimulationEvent peekNext() { return events.peek(); }
    public boolean isEmpty() { return events.isEmpty(); }
    public List<SimulationEvent> snapshot() { return events.stream().sorted(ORDER).toList(); }

    public void cancelArrivals() { events.removeIf(event -> event.type() == EventType.ARRIVAL); }
}
