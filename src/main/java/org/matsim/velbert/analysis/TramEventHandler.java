package org.matsim.velbert.analysis;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class TramEventHandler implements PersonEntersVehicleEventHandler, ActivityEndEventHandler {
    int personCounter = 0;
    int facilityCounter = 0;
    VelbertScenario scenario;

    Map<Coord, List<String>> coordToFacilityComplete = Map.ofEntries(
            Map.entry(new Coord(369218.6443704444, 5690964.620886338), List.of("tramFacility_0", "tramFacility_13")),
            Map.entry(new Coord(367661.37416265206, 5690261.726603591), List.of("tramFacility_1", "tramFacility_12")),
            Map.entry(new Coord(365609.09685220156, 5690500.130894115), List.of("tramFacility_2", "tramFacility_11")),
            Map.entry(new Coord(363674.281522717, 5689336.485888358), List.of("tramFacility_3", "tramFacility_10")),
            Map.entry(new Coord(364504.08071507426, 5688350.853096858), List.of("tramFacility_4", "tramFacility_9")),
            Map.entry(new Coord(365290.5972728388, 5686411.341331984), List.of("tramFacility_5", "tramFacility_8")),
            Map.entry(new Coord(366849.5001632444, 5686330.34422004), List.of("tramFacility_6", "tramFacility_7"))
    );

    Map<Coord, List<String>> coordToFacilityLangenberg = Map.ofEntries(
            Map.entry(new Coord(369218.6443704444, 5690964.620886338), List.of("tramFacility_0", "tramFacility_7")),
            Map.entry(new Coord(367661.37416265206, 5690261.726603591), List.of("tramFacility_1", "tramFacility_6")),
            Map.entry(new Coord(365609.09685220156, 5690500.130894115), List.of("tramFacility_2", "tramFacility_5")),
            Map.entry(new Coord(363674.281522717, 5689336.485888358), List.of("tramFacility_3", "tramFacility_4"))
    );

    Map<Coord, List<String>> coordToFacilityNeviges = Map.ofEntries(
            Map.entry(new Coord(363674.281522717, 5689336.485888358), List.of("tramFacility_0", "tramFacility_7")),
            Map.entry(new Coord(364504.08071507426, 5688350.853096858), List.of("tramFacility_1", "tramFacility_6")),
            Map.entry(new Coord(365290.5972728388, 5686411.341331984), List.of("tramFacility_2", "tramFacility_5")),
            Map.entry(new Coord(366849.5001632444, 5686330.34422004), List.of("tramFacility_3", "tramFacility_4"))
    );

    Map<Id<Person>, String> tripMap = new HashMap<>();

    List<AnalysisTrip> allTrips = new LinkedList<>();

    public TramEventHandler(VelbertScenario scenario){
        this.scenario = scenario;
    }

    private Map<Coord, List<String>> getRelevantMap(){
        switch (scenario){
            case NEVIGES: return coordToFacilityNeviges;
            case LANGENBERG: return coordToFacilityLangenberg;
            case COMPLETE: return coordToFacilityComplete;
        }
        throw new RuntimeException();
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent personEntersVehicleEvent) {

        if(personEntersVehicleEvent.getVehicleId().toString().startsWith("tram") &&
                !personEntersVehicleEvent.getPersonId().toString().startsWith("pt_tram")){
            personCounter++;
        }
    }

    @Override
    public void handleEvent(ActivityEndEvent activityEndEvent) {
        if(getRelevantMap().containsKey(activityEndEvent.getCoord())){
            facilityCounter++;

            int index;

            if(activityEndEvent.getLinkId().toString().endsWith("b")) {
                index = 1;
            }else{
                index = 0;
            }
            String stopFacility = getRelevantMap().get(activityEndEvent.getCoord()).get(1);
            pushToMap(activityEndEvent.getPersonId(), stopFacility);
        }
    }

    public void pushToMap(Id<Person> id, String stopFacility){
        if(!tripMap.containsKey(id)){
            tripMap.put(id, stopFacility);
        }else{
            allTrips.add(new AnalysisTrip(convertFacilityString(tripMap.get(id)), convertFacilityString(stopFacility)));
            tripMap.remove(id);
        }
    }

    void printCounter() throws FileNotFoundException {
        System.out.println("Person Counter: " + personCounter);
        System.out.println("Facility Counter: " + facilityCounter);

        System.out.println("Size allTrips " + allTrips.size());
        Map<AnalysisTrip, Long> tripOccurrences = allTrips.stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));

        File csvOutput = new File("C:\\Users\\paulh\\git\\Uni\\MatSim\\velbert\\analysis\\tramOutput\\" + scenario.folderName + "\\tripsPerFacilities.csv");

        try(PrintWriter pw = new PrintWriter(csvOutput)){
            pw.println("star facility; end facility; occurrences");
            tripOccurrences.entrySet().stream().map(e -> e.getKey().getStartFacility() + ";" + e.getKey().getEndFacility() + ";" + e.getValue()).forEach(pw::println);
        }
    }

    private String convertFacilityString(String facility){
        return facility;
        /*
        Long facilityId = Long.parseLong(facility.substring(13));
        if(facilityId>6){
            facilityId = 13-facilityId;
        }
        return "tramFacility"+facilityId;*/
    }

    class AnalysisTrip {
        String startFacility;
        String endFacility;

        public AnalysisTrip(String startFacility, String endFacility){
            this.startFacility = startFacility;
            this.endFacility = endFacility;
        }

        public String getEndFacility() {
            return endFacility;
        }

        public void setEndFacility(String endFacility) {
            this.endFacility = endFacility;
        }

        public String getStartFacility() {
            return startFacility;
        }

        public boolean isComplete(){
            return this.getStartFacility()!=null && this.getEndFacility()!=null;
        }

        @Override
        public String toString() {
            return "AnalysisTrip{" +
                    "startFacility='" + startFacility + '\'' +
                    ", endFacility='" + endFacility + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AnalysisTrip that = (AnalysisTrip) o;
            return Objects.equals(startFacility, that.startFacility) && Objects.equals(endFacility, that.endFacility);
        }

        @Override
        public int hashCode() {
            return Objects.hash(startFacility, endFacility);
        }
    }
}


