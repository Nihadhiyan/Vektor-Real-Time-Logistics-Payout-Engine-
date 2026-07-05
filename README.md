# Last-Mile Delivery & Payout Engine (Work in Progress)

A distributed backend pipeline designed to handle real-time logistics telemetry and batch-process daily driver payouts.

## System Architecture

This project mimics a real-world enterprise logistics backend by separating high-velocity data ingestion from heavy financial processing.

- **Real-Time Stream:** Apache Kafka (KRaft mode) ingests continuous GPS and delivery status payloads from mobile clients.
- **Batch Processing:** Spring Batch aggregates the day's event stream to calculate and generate driver payout statements.
- **Infrastructure:** Dockerized local environment.

## Current Status: Active Development 🚧

- [x] Infrastructure setup (Kafka / Docker Compose)
- [x] Telemetry data simulator (Python Producer)
- [ ] Spring Boot Kafka Consumer integration
- [ ] Spring Batch job configuration for financial calculation

## Tech Stack

- **Java 17 / Spring Boot** (Spring for Apache Kafka, Spring Batch)
- **Apache Kafka** (Message Broker)
- **Python** (Mock Data Generation)
- **Docker** (Containerization)
