import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

path_to_csv = "../../velbert-MATSim/analysis/trips/velbert-v1.0-1pct.output_trips122.csv"  # "../scenarios/equil/output-velbert-v1.0-1pct/velbert-v1.0-1pct.output_trips.csv"
path_to_txt = "../../velbert-MATSim/src/main/resources/personIdsFromVelbert.txt"

modes_full = ["bike", "car", "pt", "ride", "walk"]
modes_comp = ["bike", "car", "pt", "walk"]

desired_modal_share = {
    "bike": 0.098,
    "car": 0.563,
    "pt": 0.084,
    "walk": 0.254
}

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

modal_share = dict([(m, stat[m]) for m in modes_full])
modal_share["car"] = modal_share["car"] + modal_share["ride"]
modal_share.pop("ride")

assert abs(
    sum(modal_share.values()) - 1.0) < 0.01, f"Modal share does not sum up to 100%, but to {sum(modal_share.values())}"

print("--- Differences (desired - actual) ---")
list_desired = []
list_actual = []
for m in modes_comp:
    list_desired.append(desired_modal_share[m])
    list_actual.append(modal_share[m])
    print(f"Difference of {m}: {desired_modal_share[m] - modal_share[m]}")

x = np.arange(len(modes_comp))
width = 0.35

fig, ax = plt.subplots()
desired = ax.bar(x - width / 2, list_desired, width, label="Real World")
actual = ax.bar(x + width / 2, list_actual, width, label="MATSim")

ax.set_ylabel('share')
ax.set_title('Modal share')
ax.set_xticks(x)
ax.set_xticklabels(modes_comp)
ax.legend()

ax.bar_label(desired, padding=3)
ax.bar_label(actual, padding=3)

fig.tight_layout()

plt.show()

# plt.hist(modes_comp, bins=list(modal_share.values()))
# plt.show()
