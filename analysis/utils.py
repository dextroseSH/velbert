import pandas as pd

def get_filtered_trips(path_filtered_trips):
    with open(path_filtered_trips) as f:
        trips_filtered = f.readlines()

    return [t.strip("\n") for t in trips_filtered]


def csv_to_pd(path):
    return pd.read_csv(path, delimiter=";")


def get_sec(time_str):
    """Get Seconds from time."""
    try:
        h, m, s = time_str.split(':')
    except AttributeError:
        print(time_str)
        return 0
    return int(h) * 3600 + int(m) * 60 + int(s)
