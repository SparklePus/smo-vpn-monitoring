package smo;

import java.util.List;

public record StepResult(long number, SimulationEvent event, Request request,
                         InputDispatcher.Placement placement,
                         List<OutputDispatcher.Assignment> assignments) { }
