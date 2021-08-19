package org.matsim.velbert.analysis;

import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;

public class TramEventHandler implements PersonEntersVehicleEventHandler {
    int counter = 0;

    @Override
    public void handleEvent(PersonEntersVehicleEvent personEntersVehicleEvent) {

        /*if(personEntersVehicleEvent.getVehicleId().toString().startsWith("tr_35319")){
            System.out.print
        }*/

        if(personEntersVehicleEvent.getVehicleId().toString().startsWith("tram") &&
                !personEntersVehicleEvent.getPersonId().toString().startsWith("pt_tram")){
            counter++;
        }
    }

    void printCounter(){
        System.out.println(counter);
    }
}
