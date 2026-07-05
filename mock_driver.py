import time
import json
import random
from kafka import KafkaProducer

# Initialize connection to your local Kafka cluster
producer = KafkaProducer(
    bootstrap_servers=['localhost:9092'],
    value_serializer=lambda v: json.dumps(v).encode('utf-8')
)

driver_id = "R-101"
# Starting coordinates
lat, lng = 6.8100, 79.8700 

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
            "driverId": driver_id,
            "status": status,
            "lat": round(lat, 4),
            "lng": round(lng, 4)
        }
        
        # Publish to the 'delivery-updates' topic
        producer.send('delivery-updates', value=payload)
        print(f"Sent: {payload}")
        
        iteration += 1
        time.sleep(2) # Send a heartbeat every 2 seconds
        
except KeyboardInterrupt:
    print("\nSimulation stopped.")
    producer.close()