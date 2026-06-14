package net.vortexdevelopment.vortexcore.compatibility.folia;

import org.junit.Test;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

public class PrintExecutionStatesTest {
    @Test
    public void printStates() {
        for (ScheduledTask.ExecutionState state : ScheduledTask.ExecutionState.values()) {
            System.out.println("STATE: " + state.name());
        }
    }
}
