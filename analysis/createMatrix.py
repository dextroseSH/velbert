import numpy as np
import pandas as pd


def createMatrix(trips_before, trips_after, trips_filtered, output_path):
    modes = ["bike", "car", "pt", "ride", "walk"]

    trips_before_mask = trips_before["trip_id"].isin(trips_filtered)
    trips_after_mask = trips_after["trip_id"].isin(trips_filtered)

    trips_before = trips_before[trips_before_mask]
    trips_after = trips_after[trips_after_mask]

    all_trips = trips_before.merge(trips_after, on="trip_id", suffixes=("_before", "_after"))

    result = np.zeros((5, 5))
    for i, r in all_trips.iterrows():
        mode_before = r["longest_distance_mode_before"]
        mode_after = r["longest_distance_mode_after"]

        if mode_before is None or mode_after is None:
            continue
        assert mode_after in modes and mode_before in modes

        result[modes.index(mode_before), modes.index(mode_after)] += 1

    print(result)
    result_frame = pd.DataFrame(result)
    result_frame.columns = modes;
    result_frame.to_csv(output_path, sep=";", index=None)


def csv_to_pd(path):
    return pd.read_csv(path, delimiter=";")


path_to_before = "../trips/velbert-v1.0-1pct.output_trips9.csv"
path_to_after = "../trips/velbert-v1.0-1pct.output_trips122.csv"
path_to_filtered_trips = "../../src/main/resources/tripsBetweenVelbertAndEssen.txt"

trips_before = csv_to_pd(path_to_before)
trips_after = csv_to_pd(path_to_after)

with open(path_to_filtered_trips) as f:
    trips_filtered = f.readlines()

trips_filtered = [t.strip("\n") for t in trips_filtered]

createMatrix(trips_before, trips_after, trips_filtered, "res.csv")
