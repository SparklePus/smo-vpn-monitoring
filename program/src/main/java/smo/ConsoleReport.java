package smo;

import java.io.PrintStream;
import java.util.Locale;

final class ConsoleReport {
    private ConsoleReport() { }

    static void parameters(SimulationConfig c, PrintStream out) {
        out.println("Вариант 25 · ИБ ИЗ1 ПЗ2 Д1ОЗ3 Д1ОО3 Д2П1 Д2Б2 ОР1 ОД1");
        out.printf(Locale.ROOT, "Источники=%d | λ каждого=%.3f | приборы=%d | буфер=%d | обслуживание=[%.3f, %.3f] | N=%d | seed=%d%n",
                c.sourceCount(), c.arrivalRate(), c.workerCount(), c.bufferCapacity(), c.minServiceTime(),
                c.maxServiceTime(), c.maxRequests(), c.randomSeed());
    }

    static void summary(SimulationEngine engine, PrintStream out) {
        var stats = engine.getStatistics();
        var workers = engine.getWorkers();
        long busyWorkers = workers.stream().filter(w -> !w.isFree()).count();
        double time = engine.getModelTime();
        out.println(engine.isFinished() ? "\nМОДЕЛИРОВАНИЕ ЗАВЕРШЕНО" : "\nПРОМЕЖУТОЧНАЯ СВОДКА");
        out.printf(Locale.ROOT, "t=%.4f | шагов=%d | создано=%d | обслужено=%d | отказано=%d | БП=%d | на приборах=%d | Pотк=%s%n",
                time, engine.getStepCount(), stats.totalGenerated(), stats.totalCompleted(), stats.totalRejected(),
                engine.getBufferSize(), busyWorkers, percent(stats.totalRejected(), stats.totalGenerated()));
        out.println("Таблица 1. Источники");
        String columns = "%4s %8s %8s %8s %9s %10s %10s %10s %10s %10s%n";
        out.printf(columns, "Ист.", "Nген", "Nотк", "Nобсл", "Pотк", "Tсист", "Tож", "Tобсл", "Dож", "Dобсл");
        for (int id = 1; id <= engine.getConfig().sourceCount(); id++) {
            var s = stats.forSource(id);
            int n = s.getCompleted();
            out.printf(columns, "И" + id, s.getGenerated(), s.getRejected(), n,
                    percent(s.getRejected(), s.getGenerated()), fmt(s.meanSystem(), n > 0),
                    fmt(s.meanWaiting(), n > 0), fmt(s.meanService(), n > 0),
                    fmt(s.waitingVariance(), n >= 2), fmt(s.serviceVariance(), n >= 2));
        }
        out.println("T и D рассчитаны по завершённым заявкам. D выборочная (n−1), при n < 2 показано тире.");
        out.println("Таблица 2. Приборы");
        out.printf("%4s %8s %14s %10s%n", "Приб.", "Nобсл", "Время занят.", "Кисп");
        for (Worker w : workers) {
            double busy = w.busyTimeUntil(time);
            out.printf(Locale.ROOT, "%4s %8d %14.4f %10s%n", "П" + w.getId(), w.getCompletedCount(),
                    busy, percent(busy, time));
        }
        out.printf(Locale.ROOT, "ρ входной нагрузки=%.4f | средний Кисп=%s | Tсист обслуженных=%s%n",
                engine.getConfig().offeredLoad(), percent(meanBusyTime(engine), time),
                fmt(meanSystemTime(engine), stats.totalCompleted() > 0));
    }

    static double meanSystemTime(SimulationEngine engine) {
        var stats = engine.getStatistics();
        double sum = 0;
        for (int id = 1; id <= engine.getConfig().sourceCount(); id++) {
            var s = stats.forSource(id);
            sum += s.meanSystem() * s.getCompleted();
        }
        return stats.totalCompleted() == 0 ? 0 : sum / stats.totalCompleted();
    }

    static double meanBusyTime(SimulationEngine engine) {
        double sum = 0;
        for (Worker w : engine.getWorkers()) sum += w.busyTimeUntil(engine.getModelTime());
        return sum / engine.getConfig().workerCount();
    }

    static String percent(double numerator, double denominator) {
        return denominator == 0 ? "—" : String.format(Locale.ROOT, "%.2f%%", 100 * numerator / denominator);
    }

    private static String fmt(double value, boolean defined) {
        return defined ? String.format(Locale.ROOT, "%.4f", value) : "—";
    }
}
