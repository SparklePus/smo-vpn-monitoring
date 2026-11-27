package smo;

public final class InputDispatcher {
    public record Placement(int slot, Buffer.Selection evicted) { }

    private final Buffer buffer;
    private final StatisticsCollector statistics;

    public InputDispatcher(Buffer buffer, StatisticsCollector statistics) {
        this.buffer = buffer;
        this.statistics = statistics;
    }

    public Placement acceptRequest(Request request, double time) {
        Buffer.Selection evicted = null;
        if (buffer.isFull()) {
            evicted = buffer.evictOldest();
            evicted.request().markRejected();
            statistics.recordRejected(evicted.request());
        }
        int slot = buffer.insertFirstFree(request);
        request.markBuffered(time);
        return new Placement(slot, evicted);
    }
}
