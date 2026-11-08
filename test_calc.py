import datetime

# 2026-11-06 00:00:00 Rome time (UTC+1, so 2026-11-05 23:00:00 UTC)
# Epoch of 2026-11-05 23:00:00 UTC in ms:
# 20763 days * 86400 - 3600 = 1793923200 - 3600 = 1793919600 s = 1793919600000 ms
start_of_day_ms = 1793919600000
offset_ms = 3600000 # UTC+1

print("(start_of_day_ms + offset_ms) / 86400000 =", (start_of_day_ms + offset_ms) / 86400000)
print("Julian Day:", int((start_of_day_ms + offset_ms) / 86400000) + 2440588)
