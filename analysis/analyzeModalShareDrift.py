import numpy as np
import math
from utils import *


def createMatrix(trips_before, trips_after, trips_filtered, output_path):
    modes = ["bike", "car", "pt", "ride", "walk"]

    trips_before_mask = trips_before["trip_id"].isin(trips_filtered)
    trips_after_mask = trips_after["trip_id"].isin(trips_filtered)

    trips_before = trips_before[trips_before_mask]
    trips_after = trips_after[trips_after_mask]

    all_trips = trips_before.merge(trips_after, on="trip_id", suffixes=("_before", "_after"))

    result = np.zeros((5, 5))
    for i, r in all_trips.iterrows():
        mode_before = str(r["longest_distance_mode_before"])
        mode_after = str(r["longest_distance_mode_after"])

        if mode_before is None or mode_after is None or mode_before == "nan" or mode_after == "nan":
            continue
        assert mode_after in modes and mode_before in modes, f"mode_after: {mode_after}, mode before: {mode_before}"

        result[modes.index(mode_before), modes.index(mode_after)] += 1

    print(result)
    result_frame = pd.DataFrame(result)
    result_frame.columns = modes
    result_frame.to_csv(output_path, sep=";", index=None)


path_to_before = "tramOutput/null/class-example.output_trips.csv"

trips_between_locations = ["Wuppertal", "Essen", "Outside"]
scenarios = ["langenbergOnly", "nevigesOnly", "complete"]

for s in scenarios:
    for t in trips_between_locations:
        path_to_after = f"tramOutput/{s}/class-example.output_trips.csv"
        path_to_filtered_trips = f"tramOutput/null/tripsBetweenVelbertAnd{t}.txt"
        path_to_result_file = f"tramOutput/{s}/tripsBetweenVelbertAnd{t}Matrix.csv"

        trips_before = csv_to_pd(path_to_before)
        trips_after = csv_to_pd(path_to_after)

        trips_filtered = get_filtered_trips(path_to_filtered_trips)

        createMatrix(trips_before, trips_after, trips_filtered, path_to_result_file)

