import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

scenarios = ["langenbergOnly", "nevigesOnly", "complete"]
for s in scenarios:
    path_to_csv = f"tramOutput/{s}/class-example.output_trips.csv"
    path_to_txt = "tramOutput/null/personIdsFromVelbert.txt"

    modes_full = ["bike", "car", "pt", "ride", "walk"]

    trips = pd.read_csv(path_to_csv, delimiter=";")

    with open(path_to_txt) as f:
        persons = f.readlines()
    persons = [int(p.strip("\n")) for p in persons]

    print(f"Persons overall: {len(persons)}")
    print(f"Trips overall: {trips.shape[0]}")

    # filter trips
    personMask = trips["person"].isin(persons)
    tripsMask = trips["longest_distance_mode"].notnull()
    trips = trips[personMask & tripsMask]
    number_of_trips = trips.shape[0]

    print(f"Trips after filtering: {number_of_trips}")

    stat = trips.groupby(["longest_distance_mode"]).count()["trip_id"] / number_of_trips
    print(stat)
    stat.to_csv("test.csv", sep=";")

    modal_share = dict([(m, stat[m]) for m in modes_full])

    assert abs(
        sum(modal_share.values()) - 1.0) < 0.01, f"Modal share does not sum up to 100%, but to {sum(modal_share.values())}"
