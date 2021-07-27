package org.matsim.velbert.prepare;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class CreateTram {

	private static final TransitScheduleFactory scheduleFactory = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getTransitSchedule().getFactory();
	/**
	 * Creates Vehicles and Schedules for new tram-lines between Velbert, Neviges and Langenberg
	 * Usage:
	 * args[0]: (mandatory) schedule frequency in minutes
	 * args[1]: (mandatory) path to the output directory
	 * args[2]: (optional)  flag to generate only one line (--nevigesOnly or --langenbergOnly)
	 */

	public static int VELBERT_ZOB_INDEX = 3; // index of the first link after the Velbert-ZOB in LANGENBG_TO_NEVIGES
	public static String[] LANGENBERG_TO_NEVIGES = {"sb_01", "sb_02", "sb_03", "sb_04", "sb_05", "sb_06"};
	public static String[] NEVIGES_TO_LANGENBERG = {"sb_06b", "sb_05b", "sb_04b", "sb_03b", "sb_02b", "sb_01b"};
	private final Path root;
	private final Network network;
	private final Scenario scenario;
	private final int frequency;
	private final boolean createLangenberg;
	private final boolean createNeviges;
	private int facilityIdCounter = 0;
	private int departureAndVehicleIdCounter = 0;

	public CreateTram(Path root, int frequency, boolean createLangenberg, boolean createNeviges) {
		this.root = root;
		this.frequency = frequency;
		this.createLangenberg = createLangenberg;
		this.createNeviges = createNeviges;
//		network = NetworkUtils.readNetwork(root.resolve("matsim-velbert-v1.0.network_mit_strassenbahn.xml").toString());
//		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(root.resolve("matsim-velbert-v1.1.config.xml").toString()));
		network = scenario.getNetwork();
	}

	public static void main(String[] args) {
		if (args.length < 2) {
			throw new IllegalArgumentException();
		}
		boolean createLangenberg = args.length <= 2 || !"--nevigesOnly".equals(args[2]);
		boolean createNeviges = args.length <= 2 || !"--langenbergOnly".equals(args[2]);
		new CreateTram(Paths.get(args[1]), Integer.parseInt(args[0]), createLangenberg, createNeviges).run();
	}

	public void run() {
		VehicleType vehicleType = createVehicleType();

		List<Id<Link>> southLinkIds = Arrays.stream(LANGENBERG_TO_NEVIGES).map(id -> NetworkUtils.getLinks(network, id).get(0).getId()).collect(Collectors.toList());
		int southDirectionStart = createLangenberg ? 0 : VELBERT_ZOB_INDEX;
		int southDirectionEnd = createNeviges ? LANGENBERG_TO_NEVIGES.length - 1 : VELBERT_ZOB_INDEX - 1;
		TransitRoute southRoute = createTransitRoute("south", southLinkIds, southDirectionStart, southDirectionEnd);

		List<Id<Link>> northLinkIds = Arrays.stream(NEVIGES_TO_LANGENBERG).map(id -> NetworkUtils.getLinks(network, id).get(0).getId()).collect(Collectors.toList());
		int northDirectionStart = createNeviges ? 0 : LANGENBERG_TO_NEVIGES.length - VELBERT_ZOB_INDEX;
		int northDirectionEnd = createLangenberg ? NEVIGES_TO_LANGENBERG.length - 1 : LANGENBERG_TO_NEVIGES.length - VELBERT_ZOB_INDEX - 1;
		TransitRoute northRoute = createTransitRoute("north", northLinkIds, northDirectionStart, northDirectionEnd);

		for (int i = 0; i < 86400; i += frequency * 60) {
			northRoute.addDeparture(createDepartureAndVehicle(i, vehicleType));
			southRoute.addDeparture(createDepartureAndVehicle(i, vehicleType));
		}

		TransitLine line = scheduleFactory.createTransitLine(Id.create("Tram", TransitLine.class));
		line.addRoute(northRoute);
		line.addRoute(southRoute);
		scenario.getTransitSchedule().addTransitLine(line);

		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(root.resolve("transitSchedule.xml.gz").toString());
		new MatsimVehicleWriter(scenario.getTransitVehicles()).writeFile(root.resolve("transitVehicles.xml.gz").toString());
	}

	private Departure createDepartureAndVehicle(int departureTime, VehicleType vehicleType) {
		Vehicle vehicle = scenario.getTransitVehicles().getFactory().createVehicle(Id.createVehicleId("tram_vehicle_" + departureAndVehicleIdCounter), vehicleType);
		scenario.getTransitVehicles().addVehicle(vehicle);
		Departure departure = scheduleFactory.createDeparture(Id.create("departure_" + departureAndVehicleIdCounter++, Departure.class), departureTime);
		departure.setVehicleId(vehicle.getId());
		return departure;
	}

	private TransitRoute createTransitRoute(String id, List<Id<Link>> allLinkIdsInDirection, int startIndex, int endIndex) {
		NetworkRoute networkRoute = RouteUtils.createLinkNetworkRouteImpl(
				allLinkIdsInDirection.get(startIndex),
				allLinkIdsInDirection.subList(startIndex + 1, endIndex).toArray(new Id[0]),
				allLinkIdsInDirection.get(endIndex));
		List<TransitRouteStop> stopList = createStopListFromLinkIds(allLinkIdsInDirection.subList(startIndex, endIndex + 1));
		return scheduleFactory.createTransitRoute(Id.create(id, TransitRoute.class), networkRoute, stopList, "pt");
	}

	private List<TransitRouteStop> createStopListFromLinkIds(List<Id<Link>> linkIds) {
		List<TransitRouteStop> result = new LinkedList<>();
		result.add(scheduleFactory.createTransitRouteStop(createStopFacility(linkIds.get(0), true), 0, 0));
		for (int i = 0; i < linkIds.size(); i++) {
			result.add(scheduleFactory.createTransitRouteStop(createStopFacility(linkIds.get(i), false), (i + 1) * 240 - 30, (i + 1) * 240));
		}
		return result;
	}

	private TransitStopFacility createStopFacility(Id<Link> linkId, boolean isAtLinkStart) {
		Id<TransitStopFacility> id = Id.create("tramFacility_" + facilityIdCounter++, TransitStopFacility.class);
		Link link = network.getLinks().get(linkId);
		Node node = isAtLinkStart ? link.getFromNode() : link.getToNode();
		TransitStopFacility stopFacility = scheduleFactory.createTransitStopFacility(id, node.getCoord(), false);
		stopFacility.setLinkId(linkId);
		scenario.getTransitSchedule().addStopFacility(stopFacility);
		return stopFacility;
	}

	private VehicleType createVehicleType() {
		VehicleType vehicleType = scenario.getVehicles().getFactory().createVehicleType(Id.create("tram", VehicleType.class));
		vehicleType.setNetworkMode(TransportMode.pt);
		vehicleType.getCapacity().setStandingRoom(100);
		scenario.getTransitVehicles().addVehicleType(vehicleType);
		return vehicleType;
	}
}
