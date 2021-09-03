package org.matsim.velbert.analysis;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

public class TramAnalysis {
    public static void main(String[] args) throws FileNotFoundException {
        EventsManager manager = EventsUtils.createEventsManager();

        List<VelbertScenario> l = Arrays.asList(VelbertScenario.LANGENBERG);//, VelbertScenario.NEVIGES, VelbertScenario.COMPLETE);

        for(VelbertScenario s : l){
            TramEventHandler handler = new TramEventHandler(s);

            manager.addHandler(handler);

            EventsUtils.readEvents(manager, "C:\\Users\\paulh\\git\\Uni\\MatSim\\velbert\\analysis\\tramOutput\\" + s.folderName + "\\class-example.output_events.xml.gz");

            handler.printCounter();
        }
    }
}
