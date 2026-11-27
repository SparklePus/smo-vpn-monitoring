package smo;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record SimulationConfig(int sourceCount, int workerCount, int bufferCapacity,
                               int maxRequests, double arrivalRate, double minServiceTime,
                               double maxServiceTime, long randomSeed, int autoMaxRequests) {
    public SimulationConfig {
        if (sourceCount < 1 || workerCount < 1 || bufferCapacity < 1 || maxRequests < 1 || autoMaxRequests < 1) {
            throw new IllegalArgumentException("Число источников, приборов, ячеек и заявок должно быть положительным.");
        }
        if (!Double.isFinite(arrivalRate) || arrivalRate <= 0) {
            throw new IllegalArgumentException("Требуется конечное lambda > 0.");
        }
        if (!Double.isFinite(minServiceTime) || !Double.isFinite(maxServiceTime)
                || minServiceTime <= 0 || maxServiceTime < minServiceTime) {
            throw new IllegalArgumentException("Требуются конечные границы 0 < service.min <= service.max.");
        }
    }

    public static SimulationConfig read(Path path) throws IOException {
        Properties p = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            p.load(reader);
        }
        return new SimulationConfig(
                Integer.parseInt(required(p, "sources")), Integer.parseInt(required(p, "workers")),
                Integer.parseInt(required(p, "buffer")), Integer.parseInt(required(p, "requests")),
                Double.parseDouble(required(p, "lambda")), Double.parseDouble(required(p, "service.min")),
                Double.parseDouble(required(p, "service.max")), Long.parseLong(required(p, "seed")),
                Integer.parseInt(p.getProperty("auto.maxRequests", "1000000").trim()));
    }

    private static String required(Properties p, String key) {
        String value = p.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("В конфиге пропущен " + key);
        return value.trim();
    }

    public SimulationConfig forRun(int requests, long seed) {
        return new SimulationConfig(sourceCount, workerCount, bufferCapacity, requests, arrivalRate,
                minServiceTime, maxServiceTime, seed, autoMaxRequests);
    }

    public double offeredLoad() {
        double meanService = minServiceTime + (maxServiceTime - minServiceTime) / 2;
        return sourceCount * arrivalRate * meanService / workerCount;
    }
}
