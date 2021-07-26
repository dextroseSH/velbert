package org.matsim.velbert.analysis;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class ExportTrips implements ActivityEndEventHandler, ActivityStartEventHandler {

	private static final String pathToEventsFile = "src/main/resources/velbert-v1.0-1pct.output_events.xml";
	private static final String shapeFile = "src/main/resources/OSM_PLZ_072019.shp";
	private static final List<String> PLZ_VELBERT = Arrays.asList("42549", "42551", "42553", "42555");
	private static final List<String> PLZ_WUPPERTAL = new LinkedList<>();
	private static final List<String> PLZ_ESSEN = new LinkedList<>();
	private static String pathToOutputFile = "src/main/resources/tripsBetweenVelbertAndOutside.txt";
	private final PlzPredicate velbertPredicate;
	private final BiPredicate<Coord, Coord> tripFilter;
	private final Map<String, List<Coord>> activityCoords = new HashMap<>();

	public ExportTrips(String mode) {
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
		velbertPredicate = new PlzPredicate(features, PLZ_VELBERT);
		final Predicate<Coord> otherPredicate;
		switch (mode) {
			case "outside":
				otherPredicate = velbertPredicate.negate();
				break;
			case "wuppertal":
				otherPredicate = new PlzPredicate(features, PLZ_WUPPERTAL);
				pathToOutputFile = "src/main/resources/tripsBetweenVelbertAndWuppertal.txt";
				break;
			case "essen":
				otherPredicate = new PlzPredicate(features, PLZ_ESSEN);
				pathToOutputFile = "src/main/resources/tripsBetweenVelbertAndEssen.txt";
				break;
			default:
				otherPredicate = x -> true;
		}
		tripFilter = (start, destination) -> velbertPredicate.test(start) && otherPredicate.test(destination) || velbertPredicate.test(destination) && otherPredicate.test(start);
	}

	public static void main(String[] args) {
		for (int i = 42103; i <= 42399; i++) {
			PLZ_WUPPERTAL.add("" + i);
		}
		for (int i = 45127; i <= 45359; i++) {
			PLZ_ESSEN.add("" + i);
		}
		EventsManager manager = EventsUtils.createEventsManager();
		ExportTrips handler = new ExportTrips(args[0]);
		manager.addHandler(handler);
		EventsUtils.readEvents(manager, pathToEventsFile);
		handler.exportResult();
	}

	@Override
	public void handleEvent(ActivityEndEvent activityEndEvent) {
		String person = activityEndEvent.getPersonId().toString();
		if (!activityCoords.containsKey(person)) {
			activityCoords.put(person, new LinkedList<>());
			activityCoords.get(person).add(activityEndEvent.getCoord());
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent activityStartEvent) {
		activityCoords.get(activityStartEvent.getPersonId().toString()).add(activityStartEvent.getCoord());
	}

	private void exportResult() {
		List<String> validTripIds = new LinkedList<>();
		for (String person : activityCoords.keySet()) {
			List<Coord> coords = activityCoords.get(person);
			for (int i = 1; i < coords.size(); i++) {
				if (tripFilter.test(coords.get(i - 1), coords.get(i))) {
					validTripIds.add(person + "_" + i);
				}
			}
		}
		try (FileWriter fw = new FileWriter(pathToOutputFile)) {
			try (BufferedWriter bw = new BufferedWriter(fw)) {
				bw.write(String.join("\n", validTripIds));
				System.out.println("Found " + validTripIds.size() + " relevant trips");
				System.out.println("Wrote their IDs to " + new File(pathToOutputFile).getAbsolutePath());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
