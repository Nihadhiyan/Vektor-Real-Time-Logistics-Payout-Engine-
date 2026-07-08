from datetime import datetime, timezone
import time
import json
import random
import uuid
from kafka import KafkaProducer

# Initialize connection to your local Kafka cluster
producer = KafkaProducer(
    bootstrap_servers=['localhost:9092'],
    value_serializer=lambda v: json.dumps(v).encode('utf-8'),
    key_serializer=lambda k: k.encode('utf-8') if k is not None else None
)

driver_id = "R-101"
# Starting coordinates
lat, lng = 6.8100, 79.8700

# Mock Driver

print(f"Starting delivery simulation for {driver_id}...")
print("Press Ctrl+C to stop.")

try:
    iteration = 1
    while True:
        # Simulate slight GPS movement
        lat += random.uniform(-0.005, 0.005)
        lng += random.uniform(-0.005, 0.005)
        
        # Every 10th ping, simulate a successful delivery
        status = "DELIVERED" if iteration % 10 == 0 else "EN_ROUTE"
        
        payload = {
            "eventId": str(uuid.uuid4()),
            "driverId": driver_id,
            "status": status,
            "lat": round(lat, 4),
            "lng": round(lng, 4),
            "occurredAt": datetime.now(timezone.utc).isoformat()
        }
        
        # Publish to the 'delivery-updates' topic
        producer.send('delivery-updates', key=driver_id, value=payload)
        print(f"Sent: {payload}")
        
        iteration += 1
        time.sleep(2) # Send a heartbeat every 2 seconds
        
except KeyboardInterrupt:
    print("\nSimulation stopped.")
    producer.close()



# Maliciouse double attack test

# print("Starting malicious mock driver (Duplicate Test)...")

# duplicate_event_id = str(uuid.uuid4())

# payload = {
#     "eventId": duplicate_event_id,
#     "driverId": driver_id,
#     "status": "DELIVERED",
#     "lat": lat,
#     "lng": lng,
#     "occurredAt": "2026-07-09T12:00:00Z"
# }

# print(f"Sending original event: {duplicate_event_id}")
# producer.send('delivery-updates', value=payload)

# print(f"Sending malicious duplicate: {duplicate_event_id}")
# producer.send('delivery-updates', value=payload)

# producer.flush()
# print("Test complete. Shutting down.")