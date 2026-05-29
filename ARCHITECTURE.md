# AMPS Queue — Architecture & Design

## Overview

Maven multi-module Spring Boot 4.0.6 / Java 21 project for high-throughput order message streaming via **AMPS (Advanced Message Processing System)**.

| Module | Role |
|---|---|
| `connectors` | Shared library — AMPS client factory, publisher/subscriber abstractions, domain model |
| `amps-publisher` | Spring Boot app — REST-triggered bulk order message publisher |
| `amps-subscriber` | Spring Boot app — N subscribers × M virtual-thread workers |

---

## C4 Level 1 — System Context

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          System Context                                 │
│                                                                         │
│   [REST Client]                                                         │
│       │  POST /orders/publish                                           │
│       ▼                                                                 │
│   ┌──────────────┐   publish(topic,json)   ┌─────────────────────┐     │
│   │ amps-publisher│ ──────────────────────► │    AMPS Server      │     │
│   │ Spring Boot   │                         │  172.21.12.69:9007  │     │
│   └──────────────┘                         │  topic: orders      │     │
│                                            │  queue: orders-queue│     │
│   ┌──────────────┐   subscribe(queue)      └─────────────────────┘     │
│   │amps-subscriber◄─────────────────────────────────────────────       │
│   │ Spring Boot   │                                                     │
│   └──────────────┘                                                     │
│                                                                         │
│   [Prometheus/Grafana] ◄── scrape /actuator/prometheus (both apps)     │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## C4 Level 2 — Container Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│  amps-publisher                                                  │
│                                                                  │
│  ┌─────────────────┐    triggers    ┌──────────────────────┐    │
│  │  REST API        │ ─────────────► │ OrderPublishLoadRunner│   │
│  │  POST /orders/  │               │ 10,000 Virtual Threads│    │
│  │  publish        │               └──────────┬───────────┘    │
│  └─────────────────┘                          │ submit(json)    │
│                                               ▼                 │
│                                    ┌─────────────────────┐     │
│                                    │  PublishService      │     │
│                                    └──────────┬──────────┘     │
│                                               │ put()           │
│                                               ▼                 │
│                                    ┌─────────────────────┐     │
│                                    │ BlockingQueue<String>│     │
│                                    │  cap: 10,000         │     │
│                                    └──────────┬──────────┘     │
│                                               │ take()          │
│                                               ▼                 │
│                                    ┌─────────────────────┐     │
│                                    │  PublisherWorker     │     │
│                                    │  (single VT)         │     │
│                                    └──────────┬──────────┘     │
│                                               │                 │
│                                    ┌──────────▼──────────┐     │
│                                    │  AmpsPublisher       │     │
│                                    │  AMPS Client (1 TCP) │     │
└────────────────────────────────────┴──────────┬──────────┴─────┘
                                                 │
                                         AMPS protocol (TCP)
                                                 │
                              ┌──────────────────▼──────────────────┐
                              │          AMPS Server                 │
                              │  topic: orders                       │
                              │  queue: orders-queue                 │
                              └──────────────────┬──────────────────┘
                                                 │
                         N independent TCP subscribe connections
                          ┌──────────┬───────────┴──────────┐
                          │          │                       │
┌─────────────────────────▼──────────▼───────────────────────▼────────┐
│  amps-subscriber                                                      │
│                                                                       │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌──────────────┐ │
│  │  SubscriberGroup-1  │  │  SubscriberGroup-2  │  │ SubGroup-N   │ │
│  │  ┌───────────────┐  │  │  ┌───────────────┐  │  │  ...         │ │
│  │  │ AMPS Client-1 │  │  │  │ AMPS Client-2 │  │  │              │ │
│  │  │ (1 VT receive)│  │  │  │ (1 VT receive)│  │  │              │ │
│  │  └──────┬────────┘  │  │  └──────┬────────┘  │  │              │ │
│  │         │ put()     │  │         │ put()     │  │              │ │
│  │  ┌──────▼────────┐  │  │  ┌──────▼────────┐  │  │              │ │
│  │  │ BlockingQueue │  │  │  │ BlockingQueue │  │  │              │ │
│  │  │  cap: 50,000  │  │  │  │  cap: 50,000  │  │  │              │ │
│  │  └──────┬────────┘  │  │  └──────┬────────┘  │  │              │ │
│  │         │ take()    │  │         │ take()    │  │              │ │
│  │  ┌──────▼────────┐  │  │  ┌──────▼────────┐  │  │              │ │
│  │  │ VT-1 … VT-M   │  │  │  │ VT-1 … VT-M   │  │  │              │ │
│  │  │ MessageHandler│  │  │  │ MessageHandler│  │  │              │ │
│  │  └───────────────┘  │  │  └───────────────┘  │  │              │ │
│  └─────────────────────┘  └─────────────────────┘  └──────────────┘ │
└───────────────────────────────────────────────────────────────────────┘

         connectors (shared jar — no Spring Boot)
         ├── AmpsClientFactory
         ├── AmpsPublisher / MessagePublisher
         ├── AmpsSubscriber / MessageHandler / MessageSubscriber
         ├── OrderMessage (domain model)
         └── JsonUtils / AmpsConstants
```

---

## C4 Level 3 — Subscriber Component Detail

```
┌──────────────────────────────────────────────────────────────────────┐
│  amps-subscriber components                                          │
│                                                                      │
│  AmpsConnectionProperties (@ConfigurationProperties "amps")         │
│    host, port, clientName, topic, queue                              │
│                                                                      │
│  ConsumerProperties (@ConfigurationProperties "consumer")           │
│    subscriberCount (N), workerThreads (M), queueCapacity            │
│                                                                      │
│  AmpsSubscriberConfig (@Configuration)                               │
│    └── creates N SubscriberGroup beans via factory loop              │
│                                                                      │
│  SubscriberGroup (N instances, each isolated)                        │
│  ┌────────────────────────────────────────────────────────────┐     │
│  │  @PostConstruct start()                                    │     │
│  │    ├── AmpsClientFactory.createClient("client-{i}", ...)  │     │
│  │    ├── Thread.startVirtualThread(receiveLoop)              │     │
│  │    │     AmpsSubscriber.subscribe(queue, msg → queue.put) │     │
│  │    └── for (0..M) Thread.startVirtualThread(workerLoop)   │     │
│  │              queue.take() → MessageHandler.handle()        │     │
│  └────────────────────────────────────────────────────────────┘     │
│                                                                      │
│  OrderMessageHandler (MessageHandler impl)                           │
│    JsonUtils.fromJson(payload, OrderMessage.class) → process         │
│                                                                      │
│  SubscriberMetrics (Micrometer)                                      │
│    counters: messages_received, messages_processed, errors           │
│    timers:   processing_duration (per handler invocation)            │
└──────────────────────────────────────────────────────────────────────┘
```

---

## AMPS Message Flow

```
REST Client
    │
    │  POST /orders/publish
    ▼
PublisherController
    │
    │  publish()
    ▼
OrderPublishLoadRunner
    │  Executors.newVirtualThreadPerTaskExecutor()
    │  10,000 virtual threads each:
    │    OrderMessageFakerUtil.createOrderMessage()
    │    objectMapper.writeValueAsString(order)
    │
    │  submit(json)
    ▼
PublishService
    │  publisherQueue.put(json)          ← blocks if queue full (back-pressure)
    ▼
BlockingQueue<String>  [cap 10,000]
    │
    │  take()  (single virtual thread loop — PublisherWorker)
    ▼
AmpsPublisher.publish("orders", json)
    │
    │  client.publish(topic, payload)
    ▼
AMPS Client (TCP connection)
    │
    ▼
AMPS Server  ─────────────────────────────────────────────────────────
    │                                                                  │
    │  routes "orders" topic → "orders-queue"                          │
    │                                                                  │
    │  N parallel subscribe("orders-queue") connections                │
    ├──────────────────────────┬──────────────────────────────────    │
    │                          │                          │           │
    ▼                          ▼                          ▼           │
SubscriberGroup-1       SubscriberGroup-2       SubscriberGroup-N     │
 AMPS Client-1           AMPS Client-2           AMPS Client-N        │
 MessageStream           MessageStream           MessageStream         │
    │                          │                          │           │
    │ put()                    │ put()                    │ put()      │
    ▼                          ▼                          ▼           │
BlockingQueue          BlockingQueue          BlockingQueue           │
  [50k cap]              [50k cap]              [50k cap]             │
    │                          │                          │           │
    │ take() x M               │ take() x M               │ take() x M│
    ▼                          ▼                          ▼           │
VT-1 … VT-M            VT-1 … VT-M            VT-1 … VT-M           │
MessageHandler          MessageHandler          MessageHandler        │
  → process               → process               → process          │
────────────────────────────────────────────────────────────────────────
```

---

## Multi-Subscriber (N × M) Throughput Model

```
Total parallelism = N subscribers × M worker threads

Example config (consumer.subscriber-count=3, consumer.worker-threads=8):

AMPS Server [orders-queue]
        │
        ├──► SubscriberGroup-1  (AMPS client-1)
        │         BlockingQueue(50k)
        │         VT-1 … VT-8   ← 8 concurrent processors
        │
        ├──► SubscriberGroup-2  (AMPS client-2)
        │         BlockingQueue(50k)
        │         VT-1 … VT-8
        │
        └──► SubscriberGroup-3  (AMPS client-3)
                  BlockingQueue(50k)
                  VT-1 … VT-8

Total = 24 concurrent message processors
```

**Why N separate AMPS connections (not 1)?**
AMPS delivers each message from a `subscribe()` call to exactly one subscriber connection (queue semantics). Multiple connections compete for messages, enabling horizontal scale-out without any application-level coordination.

**Why a per-group BlockingQueue?**
The AMPS receive loop must not block on downstream processing. The queue decouples network I/O from business logic and provides per-group back-pressure.

---

## Module Dependency Graph

```
amps-queue (root POM — packaging:pom)
│
├── connectors              (jar, no Spring Boot)
│     ├── com.crankuptheamps:amps-client:5.3.3.0
│     ├── jackson-databind
│     ├── lombok
│     ├── slf4j-api
│     └── jakarta.validation-api
│
├── amps-publisher          (Spring Boot fat jar) ← depends on connectors
│     ├── spring-boot-starter-web
│     ├── spring-boot-starter-actuator
│     ├── spring-boot-starter-validation
│     ├── micrometer-registry-prometheus
│     ├── jackson-datatype-jsr310
│     ├── javafaker:1.0.2
│     └── lombok
│
└── amps-subscriber         (Spring Boot fat jar) ← depends on connectors
      ├── spring-boot-starter
      ├── spring-boot-starter-actuator
      ├── spring-boot-starter-validation
      ├── micrometer-registry-prometheus
      └── lombok
```

---

## Key Design Decisions

| Concern | Decision | Rationale |
|---|---|---|
| Publisher buffer | `ArrayBlockingQueue<String>` cap 10k | Decouples HTTP thread from AMPS publish; bounded = back-pressure |
| Publisher drain | Single virtual thread | `client.publish()` is not thread-safe per client instance |
| Subscriber scale-out | N independent AMPS client connections | AMPS queue delivers each message to one subscriber — N connections compete = N×throughput |
| Worker concurrency | M virtual threads per group | Low-overhead Java 21 VTs; M=8 default, tunable |
| Subscriber buffer | Per-group `ArrayBlockingQueue` cap 50k | Isolates groups; prevents slow handler from stalling AMPS receive |
| Shared module | `connectors` jar | Single source of truth for AMPS wiring, model, utilities |
| Message format | JSON over `OrderMessage` | Jackson + JavaTimeModule handles `Instant`; human-readable |
| Thread model | Java 21 Virtual Threads throughout | Simple blocking code; no reactive complexity |

---

## Target Configuration

### amps-publisher `application.yml`
```yaml
spring:
  application:
    name: amps-publisher
server:
  port: 9090
amps:
  client-name: publisher-client
  host: 172.21.12.69
  port: 9007
  topic: orders
  queue: orders-queue
publisher:
  queue-capacity: 10000
  batch-size: 100
```

### amps-subscriber `application.yaml`
```yaml
spring:
  application:
    name: amps-subscriber
amps:
  client-name: subscriber-client
  host: 172.21.12.69
  port: 9007
  topic: orders
  queue: orders-queue
consumer:
  subscriber-count: 3     # N — number of AMPS client connections
  worker-threads: 8       # M — virtual threads per subscriber group
  queue-capacity: 50000   # per-group BlockingQueue capacity
```

---

## Outstanding Work (Subscriber)

The subscriber module is currently a skeleton. The following needs to be built:

1. `ConsumerProperties` — add `subscriberCount` field (currently only `workerThreads`, `queueCapacity`)
2. `SubscriberGroup` — encapsulates one AMPS client + BlockingQueue + M VT workers
3. `AmpsSubscriberConfig` — Spring `@Configuration` that creates N `SubscriberGroup` beans
4. `OrderMessageHandler` — `MessageHandler` implementation with business logic
5. Micrometer metrics instrumentation
6. Clean up duplicate dependencies in `amps-subscriber/pom.xml` (spring-boot-starter and actuator declared twice)