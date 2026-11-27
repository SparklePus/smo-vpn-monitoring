package smo;

public final class StatisticsCollector {
    public static final class SourceStats {
        private int generated;
        private int rejected;
        private int completed;
        private final RunningMoments waiting = new RunningMoments();
        private final RunningMoments service = new RunningMoments();
        private final RunningMoments system = new RunningMoments();

        public int getGenerated() { return generated; }
        public int getRejected() { return rejected; }
        public int getCompleted() { return completed; }
        public double rejectionProbability() { return generated == 0 ? 0.0 : (double) rejected / generated; }
        public double meanWaiting() { return waiting.mean(); }
        public double meanService() { return service.mean(); }
        public double meanSystem() { return system.mean(); }
        public double waitingVariance() { return waiting.sampleVariance(); }
        public double serviceVariance() { return service.sampleVariance(); }
    }

    private final SourceStats[] sources;

    private int totalGenerated;

    public StatisticsCollector(int sourceCount) {
        sources = new SourceStats[sourceCount];
        for (int i = 0; i < sourceCount; i++) sources[i] = new SourceStats();
    }

    public void recordGenerated(Request request) {
        forSource(request.getSourceId()).generated++;
        totalGenerated++;
    }
    public void recordRejected(Request request) { forSource(request.getSourceId()).rejected++; }

    public void recordCompleted(Request request) {
        SourceStats stats = forSource(request.getSourceId());
        stats.completed++;
        stats.waiting.add(request.waitingTime());
        stats.service.add(request.serviceTime());
        stats.system.add(request.systemTime());
    }

    public SourceStats forSource(int sourceId) { return sources[sourceId - 1]; }
    public int totalGenerated() { return totalGenerated; }
    public int totalRejected() { int n = 0; for (SourceStats s : sources) n += s.rejected; return n; }
    public int totalCompleted() { int n = 0; for (SourceStats s : sources) n += s.completed; return n; }
    public double rejectionProbability() {
        return totalGenerated() == 0 ? 0.0 : (double) totalRejected() / totalGenerated();
    }
}
