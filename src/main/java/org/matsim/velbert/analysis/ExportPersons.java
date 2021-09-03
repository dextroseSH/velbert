package org.matsim.velbert.analysis;

import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class ExportPersons implements ActivityEndEventHandler {

    private static final String pathToEventsFile = "C:\\Users\\paulh\\git\\Uni\\MatSim\\velbert\\analysis\\tramOutput\\null\\class-example.output_events.xml.gz";
    private static final String pathToOutputFile = "src/main/resources/personIdsFromVelbert.txt";
    private static final String shapeFile = "src/main/resources/OSM_PLZ_072019.shp";
    private static final List<String> PLZ_VELBERT = Arrays.asList("42549", "42551", "42553", "42555");

    private PlzPredicate velbertPredicate;

    private final Set<String> validPersonIds = new HashSet<>();

    public ExportPersons() {
        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
        velbertPredicate = new PlzPredicate(features, PLZ_VELBERT);
    }

    public static void main(String[] args) {
        EventsManager manager = EventsUtils.createEventsManager();
        ExportPersons handler = new ExportPersons();
        manager.addHandler(handler);
        EventsUtils.readEvents(manager, pathToEventsFile);
        handler.exportResult();
    }

    @Override
    public void handleEvent(ActivityEndEvent activityEndEvent) {
        if (activityEndEvent.getActType().startsWith("home") && velbertPredicate.test(activityEndEvent.getCoord())) {
            validPersonIds.add(activityEndEvent.getPersonId().toString());
        }
    }

    private void exportResult() {
        try (FileWriter fw = new FileWriter(pathToOutputFile)) {
            try (BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write(String.join("\n", validPersonIds));
                System.out.println("Found " + validPersonIds.size() + " persons from velbert");
                System.out.println("Wrote their IDs to " + new File(pathToOutputFile).getAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
