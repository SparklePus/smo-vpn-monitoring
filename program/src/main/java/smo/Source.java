package smo;

import java.util.Random;

public final class Source {
    private final int id;
    private final double arrivalRate;
    private final Random random;
    private int nextRequestNumber = 1;

    public Source(int id, double arrivalRate, Random random) {
        this.id = id;
        this.arrivalRate = arrivalRate;
        this.random = random;
    }

    public Request generateRequest(double time, long requestId) {
        return new Request(requestId, id, nextRequestNumber++, time);
    }

    public double sampleNextInterarrival() {
        return -Math.log1p(-random.nextDouble()) / arrivalRate;
    }

    public int getId() { return id; }
}
