package smo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class OutputDispatcher {
    public record Assignment(Buffer.Selection selected, int workerId, double finishTime) { }

    private final Buffer buffer;
    private final List<Worker> workers;
    private final EventCalendar calendar;

    public OutputDispatcher(Buffer buffer, List<Worker> workers, EventCalendar calendar) {
        this.buffer = buffer;
        this.workers = workers.stream().sorted(Comparator.comparingInt(Worker::getId)).toList();
        this.calendar = calendar;
    }

    public List<Assignment> dispatchAvailable(double time) {
        if (buffer.isEmpty()) return List.of();
        List<Assignment> assignments = new ArrayList<>();
        for (Worker worker : workers) {
            if (!worker.isFree()) continue;
            Buffer.Selection selected = buffer.takeNewest(); // Д2Б2
            double finish = worker.startService(selected.request(), time);
            calendar.schedule(finish, EventType.SERVICE_COMPLETION, 0, worker.getId());
            assignments.add(new Assignment(selected, worker.getId(), finish));
            if (buffer.isEmpty()) break;
        }
        return assignments;
    }
}
