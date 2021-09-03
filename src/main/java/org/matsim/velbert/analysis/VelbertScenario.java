package org.matsim.velbert.analysis;

public enum VelbertScenario {
    COMPLETE("complete"),
    NEVIGES("nevigesOnly"),
    LANGENBERG("langenbergOnly");

    public final String folderName;

    private VelbertScenario(String folderName){
        this.folderName = folderName;
    }
}
