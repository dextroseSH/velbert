import numpy as np
import pandas as pd
from utils import *
import matplotlib.pyplot as plt


def analyze_travel_times(trips, trips_filtered, output_path):
    modes = ["bike", "car", "pt", "ride", "walk"]

    trips_mask1 = trips["trip_id"].isin(trips_filtered)
    trips_mask2 = trips["longest_distance_mode"] == "pt"

    trips_mask = trips_mask1 & trips_mask2

    trips = trips[trips_mask]

    time_cols = ["trav_time", "wait_time"]
    trips["trav_time"] = trips["trav_time"].apply(lambda x: get_sec(x)/60)
    trips["wait_time"] = trips["wait_time"].apply(lambda x: get_sec(x)/60)

    print("=== Travel times ===")
    print(trips["trav_time"].describe())
    print("=== Waiting times ===")
    print(trips["wait_time"].describe())

    b = trips.boxplot(column=["trav_time", "wait_time"])
    plt.show()


path_to_trips = "../Ablage/class-example.output_trips.csv"
path_to_filtered_trips = "../src/main/resources/tripsBetweenVelbertAndOutside.txt"

trips = csv_to_pd(path_to_trips)

trips_filtered = get_filtered_trips(path_to_filtered_trips)

analyze_travel_times(trips, trips_filtered, "res.csv")
