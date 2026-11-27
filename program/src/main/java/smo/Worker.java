package smo;

import java.util.Random;

public final class Worker {
    private final int id;
    private final double minServiceTime;
    private final double maxServiceTime;
    private final Random random;
    private Request currentRequest;
    private double busySince;
    private double busyUntil;
    private double totalBusyTime;
    private int completedCount;

    public Worker(int id, double minServiceTime, double maxServiceTime, Random random) {
        this.id = id;
        this.minServiceTime = minServiceTime;
        this.maxServiceTime = maxServiceTime;
        this.random = random;
    }

    public double startService(Request request, double time) {
        if (!isFree()) throw new IllegalStateException("Прибор П" + id + " уже занят");
        request.markProcessing(time);
        currentRequest = request;
        busySince = time;
        double duration = minServiceTime + random.nextDouble() * (maxServiceTime - minServiceTime);
        busyUntil = time + duration;
        return busyUntil;
    }

    public Request completeService(double time) {
        if (isFree()) throw new IllegalStateException("Прибор П" + id + " свободен");
        Request completed = currentRequest;
        completed.markCompleted(time);
        totalBusyTime += time - busySince;
        completedCount++;
        currentRequest = null;
        return completed;
    }

    public double busyTimeUntil(double time) {
        return totalBusyTime + (isFree() ? 0.0 : Math.max(0.0, time - busySince));
    }

    public boolean isFree() { return currentRequest == null; }
    public int getId() { return id; }
    public Request getCurrentRequest() { return currentRequest; }
    public double getBusyUntil() { return busyUntil; }
    public int getCompletedCount() { return completedCount; }
}
