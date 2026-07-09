# Vektor Dispatch & Payout Engine

A real-time, fault-tolerant financial settlement engine built for distributed logistics. 

This project mimics a real-world enterprise logistics backend by separating high-velocity data ingestion from heavy financial processing. It ingests driver delivery telemetry via **Apache Kafka**, enforces database-level idempotency to prevent double-payments, and executes asynchronous financial settlements using **Spring Batch**. 

## 🏗 Architecture

```mermaid
graph TD
    subgraph Client
        M[Python Mock Driver] -->|JSON Payload| K
    end

    subgraph Docker Internal Network
        K[Apache Kafka <br/>KRaft Mode] -->|DeliveryEventUpdateRequest| C
        
        subgraph Spring Boot Microservice
            C[Kafka Consumer] -->|Idempotent Write| DB[(PostgreSQL)]
            B[Spring Batch Job] -.->|Reads Unpaid| DB
            B -->|Calculates $5.00 Payout| DB
            API[REST Controller] -.->|Fetch Statements| DB
        end
    end