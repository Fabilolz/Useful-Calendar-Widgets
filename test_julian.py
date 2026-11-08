import datetime

# Unix Epoch 1970-01-01
# Julian Day for 1970-01-01 is 2440588 in astronomical / Android Time.
epoch = datetime.date(1970, 1, 1)
d = datetime.date(2026, 11, 6)
diff = (d - epoch).days
print("Days since epoch:", diff)
print("Julian Day:", diff + 2440588)
