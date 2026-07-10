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
        
        if iteration % 15 == 0:
            bad_payload = {"driverId": driver_id, "broken_json": "missing_fields", "lat": "THIS_WILL_CRASH_JAVA"}
            producer.send('delivery-updates', key=driver_id, value=bad_payload)
            print(f"Sent Poison Pill: {bad_payload}")
        else: 
            payload = {
                "eventId": str(uuid.uuid4()),
                "driverId": driver_id,
                "status": status,
                "lat": round(lat, 4),
                "lng": round(lng, 4),
                "distanceKm": round(random.uniform(1.2, 8.5), 2),
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