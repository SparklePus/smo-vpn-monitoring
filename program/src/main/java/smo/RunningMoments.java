package smo;

final class RunningMoments {
    private int count;
    private double mean;
    private double sumSquaredDeviations;

    void add(double value) {
        count++;
        double delta = value - mean;
        mean += delta / count;
        sumSquaredDeviations += delta * (value - mean);
    }

    int count() { return count; }
    double mean() { return count == 0 ? 0.0 : mean; }
    double sampleVariance() { return count < 2 ? 0.0 : sumSquaredDeviations / (count - 1); }
}
