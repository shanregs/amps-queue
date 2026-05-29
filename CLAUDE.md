# amps-queue

## Project Overview

Maven multi-module Spring Boot 4.0.6 / Java 21 project for high-throughput order message streaming via **AMPS (Advanced Message Processing System)**.

See [ARCHITECTURE.md](ARCHITECTURE.md) for C4 diagrams, AMPS message flow, and N×M multi-subscriber design.

## Tech Stack

- **Java**: 21 (Virtual Threads throughout)
- **Spring Boot**: 4.0.6
- **AMPS client**: `com.crankuptheamps:amps-client:5.3.3.0`
- **Build**: Maven multi-module
- **Metrics**: Micrometer + Prometheus

## Modules

| Module | Type | Role |
|---|---|---|
| `connectors` | `jar` (no Spring Boot) | Shared AMPS client factory, publisher/subscriber abstractions, `OrderMessage` model, `JsonUtils` |
| `amps-publisher` | Spring Boot fat jar | REST-triggered bulk publisher — 10k VTs → BlockingQueue → single-VT drain → AMPS |
| `amps-subscriber` | Spring Boot fat jar | N subscriber groups × M VT workers — to be completed |

## Key Source Locations

### connectors
- `connectors/…/factory/AmpsClientFactory.java` — connects TCP, logs on
- `connectors/…/publisher/AmpsPublisher.java` — wraps `client.publish()`
- `connectors/…/subscriber/AmpsSubscriber.java` — blocking `MessageStream` loop
- `connectors/…/model/OrderMessage.java` — shared domain model
- `connectors/…/common/util/JsonUtils.java` — Jackson helper

### amps-publisher
- `config/AMPSConfig.java` — creates `Client` and `AmpsPublisher` beans
- `config/AmpsConnectionProperties.java` — `amps.*` config properties
- `config/PublisherProperties.java` — `publisher.queue-capacity`, `publisher.batch-size`
- `config/QueueConfig.java` — creates `BlockingQueue<String>` (ArrayBlockingQueue, cap 10k)
- `service/PublishService.java` — `submit(json)` → `queue.put()`
- `worker/PublisherWorker.java` — single VT drains queue → `AmpsPublisher.publish()`
- `runner/OrderPublishLoadRunner.java` — 10k virtual-thread load generator
- `controller/PublisherController.java` — `POST /orders/publish`
- `util/OrderMessageFakerUtil.java` — Faker-based `OrderMessage` generator

### amps-subscriber (skeleton — needs completion)
- `config/AmpsConnectionProperties.java` — `amps.*` config properties
- **TODO**: `ConsumerProperties` (add `subscriberCount` field)
- **TODO**: `SubscriberGroup` (AMPS client + BlockingQueue + M VT workers)
- **TODO**: `AmpsSubscriberConfig` (creates N `SubscriberGroup` beans)
- **TODO**: `OrderMessageHandler` (business logic)

## AMPS Connection

```
TCP: tcp://{host}:{port}/amps/json
Default: tcp://172.21.12.69:9007/amps/json
Topic:  orders
Queue:  orders-queue
```

## Publisher Flow

```
POST /orders/publish
  → 10,000 VTs generate OrderMessages
  → PublishService.submit(json) → BlockingQueue (cap 10k)
  → PublisherWorker (single VT) → AmpsPublisher → AMPS Server
```

## Subscriber Design (N × M)

```
AMPS Server [orders-queue]
  ├──► SubscriberGroup-1  (AMPS client-1) → BlockingQueue(50k) → VT-1…VT-M
  ├──► SubscriberGroup-2  (AMPS client-2) → BlockingQueue(50k) → VT-1…VT-M
  └──► SubscriberGroup-N  (AMPS client-N) → BlockingQueue(50k) → VT-1…VT-M
```

Config keys: `consumer.subscriber-count` (N), `consumer.worker-threads` (M), `consumer.queue-capacity`.

## Build

```powershell
# From root — build all modules
mvn clean install

# Skip tests
mvn clean install -DskipTests

# Validate parent resolution
mvn validate

# Run publisher
cd amps-publisher && mvn spring-boot:run

# Run subscriber
cd amps-subscriber && mvn spring-boot:run
```

## POM Conventions

- Child modules **must not** redeclare `groupId` or `version` — inherited from root.
- Parent `<version>` in child POMs must match root exactly: `0.0.1-SNAPSHOT`.
- `connectors` must **not** use `spring-boot-maven-plugin` (it is a plain jar, not a fat jar).
- Do not add `dependencyManagement` BOM imports in children — root parent handles it.

## Known Issues

- `amps-subscriber/pom.xml` has duplicate `spring-boot-starter` and `spring-boot-starter-actuator` declarations — clean up before next build.
- `connectors/pom.xml` redeclares `groupId` and `version` (redundant but harmless).
- `AmpsClientFactory.createClient` is declared `static` in implementation but called via `new AmpsClientFactory().createClient(...)` in publisher — make it consistent.