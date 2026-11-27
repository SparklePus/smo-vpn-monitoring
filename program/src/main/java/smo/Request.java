package smo;

public final class Request {
    private final long id;
    private final int sourceId;
    private final int sequenceNumber;
    private final double createdAt;
    private double bufferedAt = Double.NaN;
    private double serviceStartedAt = Double.NaN;
    private double completedAt = Double.NaN;
    private RequestStatus status = RequestStatus.CREATED;

    public Request(long id, int sourceId, int sequenceNumber, double createdAt) {
        this.id = id;
        this.sourceId = sourceId;
        this.sequenceNumber = sequenceNumber;
        this.createdAt = createdAt;
    }

    public void markBuffered(double time) {
        requireStatus(RequestStatus.CREATED);
        bufferedAt = time;
        status = RequestStatus.BUFFERED;
    }

    public void markProcessing(double time) {
        requireStatus(RequestStatus.BUFFERED);
        serviceStartedAt = time;
        status = RequestStatus.PROCESSING;
    }

    public void markCompleted(double time) {
        requireStatus(RequestStatus.PROCESSING);
        completedAt = time;
        status = RequestStatus.COMPLETED;
    }

    public void markRejected() {
        requireStatus(RequestStatus.BUFFERED);
        status = RequestStatus.REJECTED;
    }

    private void requireStatus(RequestStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Заявка " + label() + ": ожидался статус " + expected + ", получен " + status);
        }
    }

    public String label() { return sourceId + "." + sequenceNumber; }
    public double waitingTime() { return serviceStartedAt - bufferedAt; }
    public double serviceTime() { return completedAt - serviceStartedAt; }
    public double systemTime() { return completedAt - createdAt; }
    public long getId() { return id; }
    public int getSourceId() { return sourceId; }
    public int getSequenceNumber() { return sequenceNumber; }
    public double getCreatedAt() { return createdAt; }
    public double getBufferedAt() { return bufferedAt; }
    public double getServiceStartedAt() { return serviceStartedAt; }
    public double getCompletedAt() { return completedAt; }
    public RequestStatus getStatus() { return status; }
}
