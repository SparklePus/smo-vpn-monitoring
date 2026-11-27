package smo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class SimulationEngine {
    private final SimulationConfig config;
    private final List<Source> sources = new ArrayList<>();
    private final List<Worker> workers = new ArrayList<>();
    private final EventCalendar calendar = new EventCalendar();
    private final Buffer buffer;
    private final StatisticsCollector statistics;
    private final InputDispatcher inputDispatcher;
    private final OutputDispatcher outputDispatcher;
    private double modelTime;
    private long stepCount;

    public SimulationEngine(SimulationConfig config) {
        this.config = config;
        Random seeds = new Random(config.randomSeed());
        for (int id = 1; id <= config.sourceCount(); id++) {
            Source source = new Source(id, config.arrivalRate(), new Random(seeds.nextLong()));
            sources.add(source);
            scheduleArrival(source);
        }
        for (int id = 1; id <= config.workerCount(); id++) {
            workers.add(new Worker(id, config.minServiceTime(), config.maxServiceTime(), new Random(seeds.nextLong())));
        }
        buffer = new Buffer(config.bufferCapacity());
        statistics = new StatisticsCollector(config.sourceCount());
        inputDispatcher = new InputDispatcher(buffer, statistics);
        outputDispatcher = new OutputDispatcher(buffer, workers, calendar);
    }

    public StepResult step() {
        SimulationEvent event = calendar.pollNext();
        if (event == null) throw new IllegalStateException("Календарь пуст");
        modelTime = event.time();
        Request request;
        InputDispatcher.Placement placement = null;
        if (event.type() == EventType.ARRIVAL) {
            Source source = sources.get(event.sourceId() - 1);
            request = source.generateRequest(modelTime, (long) statistics.totalGenerated() + 1);
            statistics.recordGenerated(request);
            if (statistics.totalGenerated() < config.maxRequests()) {
                scheduleArrival(source);
            } else {
                calendar.cancelArrivals();
            }
            placement = inputDispatcher.acceptRequest(request, modelTime);
        } else {
            Worker worker = workers.get(event.workerId() - 1);
            request = worker.completeService(modelTime);
            statistics.recordCompleted(request);
        }
        var assignments = outputDispatcher.dispatchAvailable(modelTime);
        return new StepResult(++stepCount, event, request, placement, assignments);
    }

    private void scheduleArrival(Source source) {
        calendar.schedule(modelTime + source.sampleNextInterarrival(), EventType.ARRIVAL, source.getId(), 0);
    }

    public void runToEnd() {
        while (!isFinished()) step();
    }

    public boolean isBeforeEviction() {
        SimulationEvent next = calendar.peekNext();
        return next != null && next.type() == EventType.ARRIVAL && buffer.isFull()
                && workers.stream().noneMatch(Worker::isFree);
    }

    public Buffer.Selection evictionCandidate() { return buffer.peekOldest(); }
    public boolean isFinished() { return calendar.isEmpty(); }
    public SimulationConfig getConfig() { return config; }
    public double getModelTime() { return modelTime; }
    public long getStepCount() { return stepCount; }
    public List<Worker> getWorkers() { return List.copyOf(workers); }
    public Request[] getBufferSnapshot() { return buffer.snapshot(); }
    public int getBufferSize() { return buffer.size(); }
    public List<SimulationEvent> getCalendarSnapshot() { return calendar.snapshot(); }
    public StatisticsCollector getStatistics() { return statistics; }
}
