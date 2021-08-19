package org.matsim.velbert.analysis;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;

public class TramAnalysis {
    public static void main(String[] args){
        EventsManager manager = EventsUtils.createEventsManager();
        TramEventHandler handler = new TramEventHandler();

        manager.addHandler(handler);

        EventsUtils.readEvents(manager, "C:\\Users\\paulh\\git\\Uni\\MatSim\\velbert\\Ablage\\class-example.output_events.xml.gz");

        handler.printCounter();

    }
}
