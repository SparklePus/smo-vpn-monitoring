package smo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Locale;
import java.util.StringJoiner;

public final class ConsoleUI {
    private final SimulationEngine engine;
    private final BufferedReader input;
    private final PrintStream out;

    public ConsoleUI(SimulationEngine engine, BufferedReader input, PrintStream out) {
        this.engine = engine;
        this.input = input;
        this.out = out;
    }

    public void start() throws IOException {
        out.println("ПОШАГОВЫЙ РЕЖИМ · ОД1");
        ConsoleReport.parameters(engine.getConfig(), out);
        out.println("Enter — шаг | n 5 — несколько шагов | b — до выбивания | a — до конца | q — сводка и выход");
        printState();
        while (!engine.isFinished()) {
            out.print("> ");
            out.flush();
            String line = input.readLine();
            if (line == null || line.trim().equalsIgnoreCase("q")) break;
            String command = line.trim().toLowerCase(Locale.ROOT);
            if (command.isEmpty()) {
                printStep(engine.step());
            } else if (command.equals("b")) {
                while (!engine.isFinished() && !engine.isBeforeEviction()) engine.step();
                out.println(engine.isBeforeEviction() ? "Остановка перед выбиванием. Enter — проверить."
                        : "До конца прогона вытеснения больше нет.");
            } else if (command.equals("a")) {
                engine.runToEnd();
                break;
            } else if (command.startsWith("n ")) {
                try {
                    int count = Integer.parseInt(command.substring(2).trim());
                    if (count < 1) throw new NumberFormatException();
                    for (int i = 0; i < count && !engine.isFinished(); i++) {
                        printStep(engine.step());
                        printState();
                    }
                    continue;
                } catch (NumberFormatException e) {
                    out.println("Введите положительное число шагов, например n 5.");
                    continue;
                }
            } else {
                out.println("Команды: Enter, n 5, b, a, q.");
                continue;
            }
            printState();
        }
        ConsoleReport.summary(engine, out);
    }

    private void printStep(StepResult step) {
        SimulationEvent event = step.event();
        out.printf(Locale.ROOT, "%nШаг %d | t=%.4f | ", step.number(), event.time());
        if (event.type() == EventType.ARRIVAL) {
            var placement = step.placement();
            out.printf("И%d создал %s", event.sourceId(), step.request().label());
            if (placement.evicted() != null) {
                Buffer.Selection old = placement.evicted();
                out.printf(" | Д1ОО3: отказ %s (И%d), Б%d", old.request().label(), old.request().getSourceId(), old.slot());
            }
            out.printf(" | Д1ОЗ3: %s → Б%d%n", step.request().label(), placement.slot());
        } else {
            out.printf("П%d завершил %s%n", event.workerId(), step.request().label());
        }
        for (var assignment : step.assignments()) {
            out.printf(Locale.ROOT, "  Д2Б2: %s из Б%d → П%d (Д2П1), конец t=%.4f%n",
                    assignment.selected().request().label(), assignment.selected().slot(),
                    assignment.workerId(), assignment.finishTime());
        }
    }

    private void printState() {
        var stats = engine.getStatistics();
        out.printf(Locale.ROOT, "%nСостояние после шага %d | t=%.4f | N=%d | отказов=%d | Pотк=%s%n",
                engine.getStepCount(), engine.getModelTime(), stats.totalGenerated(), stats.totalRejected(),
                ConsoleReport.percent(stats.totalRejected(), stats.totalGenerated()));
        StringJoiner sources = new StringJoiner("   ");
        for (int id = 1; id <= engine.getConfig().sourceCount(); id++) {
            var s = stats.forSource(id);
            sources.add("И" + id + "=" + s.getGenerated() + "/" + s.getRejected() + "/" + s.getCompleted());
        }
        out.println("Источники [создано/отказ/обслужено]  " + sources);
        StringJoiner buffer = new StringJoiner("   ");
        Request[] slots = engine.getBufferSnapshot();
        for (int i = 0; i < slots.length; i++) {
            Request r = slots[i];
            String label = r == null ? "—" : String.format(Locale.ROOT, "%s@%.4f", r.label(), r.getCreatedAt());
            buffer.add("Б" + (i + 1) + "=" + label);
        }
        out.printf("Буфер [%d/%d]  %s%n", engine.getBufferSize(), slots.length, buffer);
        var workers = engine.getWorkers();
        StringJoiner devices = new StringJoiner("   ");
        for (Worker w : workers) {
            devices.add("П" + w.getId() + "=" + (w.isFree() ? "свободен" : w.getCurrentRequest().label()));
        }
        out.println("Приборы  " + devices);
        out.println("Календарь будущих событий");
        out.printf("  %10s | %-19s | %-7s | %s%n", "Время", "Событие", "Элемент", "Заявка");
        for (SimulationEvent e : engine.getCalendarSnapshot()) {
            boolean arrival = e.type() == EventType.ARRIVAL;
            String label = arrival ? e.sourceId() + "." + (stats.forSource(e.sourceId()).getGenerated() + 1L)
                    : workers.get(e.workerId() - 1).getCurrentRequest().label();
            out.printf(Locale.ROOT, "  %10.4f | %-19s | %-7s | %s%n", e.time(),
                    arrival ? "Поступление" : "Конец обслуживания",
                    arrival ? "И" + e.sourceId() : "П" + e.workerId(), label);
        }
        if (engine.isFinished()) out.println("  (пусто)");
        if (engine.isBeforeEviction()) {
            var victim = engine.evictionCandidate();
            out.printf(Locale.ROOT, "Прогноз Д1ОО3: %s в Б%d, поступила в t=%.4f — самая старая.%n",
                    victim.request().label(), victim.slot(), victim.request().getCreatedAt());
        }
    }
}
