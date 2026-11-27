package smo;

import java.io.PrintStream;
import java.util.Locale;
import java.util.Random;

final class AutomaticMode {
    static final double T_ALPHA = 1.643;
    static final double DELTA = 0.1;
    private static final int MAX_RUNS = 24;

    record Result(SimulationEngine engine, boolean converged) { }

    static long requiredRequests(double p) {
        return (long) Math.ceil(T_ALPHA * T_ALPHA * (1 - p) / (p * DELTA * DELTA));
    }

    static Result run(SimulationConfig config, PrintStream out) {
        if (config.maxRequests() > config.autoMaxRequests()) {
            throw new IllegalArgumentException("Для auto задайте requests <= auto.maxRequests.");
        }
        out.println("АВТОМАТИЧЕСКИЙ РЕЖИМ · ОР1");
        ConsoleReport.parameters(config, out);
        out.println("α=0.90 | δ=0.10 | tα=1.643 | p = отказы / созданные заявки всех источников");
        out.printf("%4s %9s %9s %11s %12s %12s%n", "№", "N факт.", "Отказов", "p", "N* по p", "Δp/pпред");
        Random seeds = new Random(config.randomSeed());
        long seed = config.randomSeed();
        int n = config.maxRequests();
        double previousP = Double.NaN;
        long previousRequired = 0;
        for (int iteration = 1; ; iteration++) {
            SimulationEngine engine = new SimulationEngine(config.forRun(n, seed));
            engine.runToEnd();
            var stats = engine.getStatistics();
            double p = stats.rejectionProbability();
            long required = p > 0 ? Math.max(1, requiredRequests(p)) : 0;
            double change = previousP > 0 ? Math.abs(p - previousP) / previousP : Double.NaN;
            out.printf(Locale.ROOT, "%4d %9d %9d %11.6f %12s %12s%n", iteration,
                    stats.totalGenerated(), stats.totalRejected(), p, p > 0 ? Long.toString(required) : "—",
                    Double.isNaN(change) ? "—" : ConsoleReport.percent(change, 1));
            if (p > 0 && change < DELTA && n >= previousRequired && n >= required) {
                return new Result(engine, true);
            }
            if (iteration == MAX_RUNS || (n == config.autoMaxRequests() && (p == 0 || required > n))) {
                out.println("Критерий точности не подтверждён: достигнут предел заявок или числа прогонов.");
                return new Result(engine, false);
            }
            long next = p == 0 ? 2L * n : Math.max(n, required);
            n = (int) Math.min(config.autoMaxRequests(), next);
            previousP = p;
            previousRequired = required;
            seed = seeds.nextLong();
        }
    }
}
