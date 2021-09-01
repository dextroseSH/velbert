package org.matsim.velbert.prepare;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetWriter;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CreateDrtVehicles {

    /**
     * Adjust these variables and paths to your need.
     */

    private static final int numberOfVehicles = 50;
    private static final int seatsPerVehicle = 6; //this is important for DRT, value is not used by taxi
    private static final double operationStartTime = 0;
    private static final double operationEndTime = 24 * 60 * 60; //24h
    private static final Random random = MatsimRandom.getRandom();

    private static final Path outputFile = Paths.get("scenarios/equil/drt/drtVehicles.xml");
    private static final Path shapeFile = Paths.get("scenarios/equil/drt/velbert.shp");

    public static void main(String[] args) {

        new CreateDrtVehicles().run();
    }

    private void run() {

        Set<Geometry> velbertGeometries = ShapeFileReader.getAllFeatures(shapeFile.toString()).stream()
                .map(f->(Geometry) f.getDefaultGeometry())
                .collect(Collectors.toSet());

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/velbert/velbert-v1.0/input/matsim-velbert-v1.0.network.xml.gz");
        final int[] i = {0};
        Stream<DvrpVehicleSpecification> vehicleSpecificationStream = scenario.getNetwork().getLinks().entrySet().stream()
                .filter(entry -> entry.getValue().getAllowedModes().contains(TransportMode.car)) // drt can only start on links with Transport mode 'car'
                .filter(entry -> velbertGeometries.size() > 0 && velbertGeometries.stream().anyMatch(g->g.covers(MGC.coord2Point(entry.getValue().getCoord()))))
                .sorted((e1, e2) -> (random.nextInt(2) - 1)) // shuffle links
                .limit(numberOfVehicles) // select the first *numberOfVehicles* links
                .map(entry -> ImmutableDvrpVehicleSpecification.newBuilder()
                        .id(Id.create("drt_" + i[0]++, DvrpVehicle.class))
                        .startLinkId(entry.getKey())
                        .capacity(seatsPerVehicle)
                        .serviceBeginTime(operationStartTime)
                        .serviceEndTime(operationEndTime)
                        .build());

        new FleetWriter(vehicleSpecificationStream).write(outputFile.toString());
    }
}
