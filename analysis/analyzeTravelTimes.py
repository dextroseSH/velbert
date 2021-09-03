from utils import *
import matplotlib.pyplot as plt


def analyze_travel_times(trips, trips_filtered, output_path):
    modes = ["bike", "car", "pt", "ride", "walk"]

    trips_mask1 = trips["trip_id"].isin(trips_filtered)
    trips_mask2 = trips["longest_distance_mode"] == "car"

    trips_mask = trips_mask1 & trips_mask2

    trips = trips[trips_mask]

    trips["trav_time"] = trips["trav_time"].apply(lambda x: get_sec(x)/60)
    trips["wait_time"] = trips["wait_time"].apply(lambda x: get_sec(x)/60)

    print("=== Travel times ===")
    trav_time_desc = trips["trav_time"].describe()
    print(trav_time_desc)
    trav_time_desc.to_csv(output_path + "travTime.csv", sep=";")

    print("=== Waiting times ===")
    wait_time_desc = trips["wait_time"].describe()
    print(wait_time_desc)
    wait_time_desc.to_csv(output_path + "waitTime.csv", sep=";")

    b = trips.boxplot(column=["trav_time", "wait_time"])
    plt.savefig(output_path + ".png")
    plt.close()


trips_between_locations = ["Wuppertal", "Essen", "Outside"]
scenarios = ["langenbergOnly", "nevigesOnly", "complete", "null"]

for s in scenarios:
    for t in trips_between_locations:

        path_to_trips = f"tramOutput/{s}/class-example.output_trips.csv"
        path_to_filtered_trips = f"tramOutput/null/tripsBetweenVelbertAnd{t}.txt"

        trips = csv_to_pd(path_to_trips)

        trips_filtered = get_filtered_trips(path_to_filtered_trips)

        analyze_travel_times(trips, trips_filtered, f"tramOutput/{s}/times/tripsBetweenVelbertAnd{t}")