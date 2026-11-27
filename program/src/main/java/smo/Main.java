package smo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        try {
            String mode = args.length == 0 ? "step" : args[0];
            switch (mode) {
                case "step" -> {
                    if (args.length > 2) throw new IllegalArgumentException("step принимает один конфиг.");
                    Path path = Path.of(args.length < 2 ? "configs/step.properties" : args[1]);
                    SimulationEngine engine = new SimulationEngine(SimulationConfig.read(path));
                    new ConsoleUI(engine, new BufferedReader(new InputStreamReader(System.in)), System.out).start();
                }
                case "auto" -> runAutomatic(args.length < 2 ? List.of("configs/auto.properties")
                        : Arrays.asList(args).subList(1, args.length));
                default -> throw new IllegalArgumentException("Режимы: step [конфиг] или auto [конфиги].");
            }
        } catch (IOException | IllegalArgumentException ex) {
            System.err.println("Ошибка: " + ex.getMessage());
            System.exit(1);
        }
    }

    private static void runAutomatic(List<String> paths) throws IOException {
        List<AutomaticMode.Result> results = new ArrayList<>();
        for (String file : paths) {
            var result = AutomaticMode.run(SimulationConfig.read(Path.of(file)), System.out);
            ConsoleReport.summary(result.engine(), System.out);
            results.add(result);
        }
        if (results.size() < 2) return;
        System.out.println("\nСРАВНЕНИЕ КОНФИГУРАЦИЙ");
        System.out.printf("%-24s %4s %4s %4s %7s %7s %7s %9s %9s %9s %9s %9s %9s%n",
                "Конфиг", "Ист.", "Приб.", "БП", "λ/ист.", "a", "b", "ρ", "N факт.", "Pотк", "ср.Кисп", "Tсист", "Критерий");
        for (int i = 0; i < results.size(); i++) {
            var result = results.get(i);
            var e = result.engine();
            var c = e.getConfig();
            var s = e.getStatistics();
            System.out.printf(Locale.ROOT, "%-24s %4d %4d %4d %7.3f %7.3f %7.3f %9.4f %9d %9s %9s %9.4f %9s%n",
                    Path.of(paths.get(i)).getFileName(), c.sourceCount(), c.workerCount(), c.bufferCapacity(),
                    c.arrivalRate(), c.minServiceTime(), c.maxServiceTime(), c.offeredLoad(), s.totalGenerated(),
                    ConsoleReport.percent(s.totalRejected(), s.totalGenerated()),
                    ConsoleReport.percent(ConsoleReport.meanBusyTime(e), e.getModelTime()),
                    ConsoleReport.meanSystemTime(e), result.converged() ? "да" : "нет");
        }
    }
}
