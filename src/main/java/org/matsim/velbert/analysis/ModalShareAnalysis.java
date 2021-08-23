package org.matsim.velbert.analysis;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.util.*;
import java.util.stream.Collectors;

public class ModalShareAnalysis {

	private static final String shapeFile = "src/main/resources/OSM_PLZ_072019.shp";
	private static final String populationPath = "src/main/resources/velbert-v1.0-1pct.output_plans.xml.gz";
	private static final CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("EPSG:25832", "EPSG:3857");
	private static final List<String> plzVelbert = Arrays.asList("42549", "42551", "42553", "42555");

	public static void main(String[] args) {
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
		Population population = PopulationUtils.readPopulation(populationPath);
		Map<String, Integer> modalCounter = new HashMap<>();

		List<Geometry> velbert = features.stream()
				.filter(feature -> plzVelbert.contains(feature.getAttribute("plz")))
				.map(feature -> (Geometry) feature.getDefaultGeometry())
				.collect(Collectors.toList());

		int personCounter = 0; // persons living in velbert
		int tripCounter = 0; // number of trips of people living in velbert
		for (Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();

			List<Activity> activities = TripStructureUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
			boolean livingInVelbert = activities.stream()
					.filter(a -> a.getType().startsWith("home"))
					.map(Activity::getCoord)
					.map(transformation::transform)
					.map(MGC::coord2Point)
					.allMatch(coord -> velbert.stream().anyMatch(plz -> plz.covers(coord)));
			if (!livingInVelbert) {
				continue;
			}


			personCounter++;

			List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan);
			tripCounter += trips.size();
			for (TripStructureUtils.Trip trip : trips) {
				List<String> modes = trip.getLegsOnly().stream().map(Leg::getMode).distinct().collect(Collectors.toList());
				String mode = null;
				for (String possibleMode : modes) {
					if (mode == null || "walk".equals(mode)) {
						mode = possibleMode;
					}
				}
				if (modalCounter.containsKey(mode)) {
					modalCounter.put(mode, modalCounter.get(mode) + 1);
				} else {
					modalCounter.put(mode, 1);
				}
			}
		}
		System.out.println(personCounter + " people live in Velbert");
		System.out.println("These people do " + tripCounter + " trips");
		System.out.println("The modal share:");
		for (String mode : modalCounter.keySet()) {
			int countResult = modalCounter.get(mode);
			System.out.println(mode + "\t" + countResult + " trips\t" + (countResult * 100.0 / tripCounter) + "%");
		}
	}
}
