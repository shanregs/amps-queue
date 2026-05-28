# How to Run AMPS Server on Windows 11
### With Spring Boot Producer/Consumer Integration

> **AMPS Server (60East Technologies)** does not run natively on Windows.
> The recommended approach is to run it inside **WSL2** (Windows Subsystem for Linux),
> which is built into Windows 11 and used by 60East engineers themselves.

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Install WSL2](#2-install-wsl2)
3. [Open WSL Shell](#3-open-wsl-shell)
4. [Download and Install AMPS Server](#4-download-and-install-amps-server)
5. [Create AMPS Config with a Queue](#5-create-amps-config-with-a-queue)
6. [Start AMPS Server](#6-start-amps-server)
7. [Find Your WSL2 IP (for Windows connectivity)](#7-find-your-wsl2-ip)
8. [Verify Connectivity from Windows](#8-verify-connectivity-from-windows)
9. [Spring Boot Integration](#9-spring-boot-integration)
10. [Common Issues and Fixes](#10-common-issues-and-fixes)
11. [Quick Reference Cheat Sheet](#11-quick-reference-cheat-sheet)

---

## 1. Prerequisites

| Requirement | Notes |
|---|---|
| Windows 11 (Home or Pro) | S Mode not supported |
| Hardware Virtualization enabled | Check in Task Manager → Performance → CPU → "Virtualization: Enabled" |
| Internet connection | Required for WSL2 and AMPS download |
| Java 17+ (on Windows) | For your Spring Boot application |
| Maven or Gradle (on Windows) | For building the Spring Boot project |

---

## 2. Install WSL2

Open **PowerShell as Administrator** (right-click Start → "Windows PowerShell (Admin)") and run:

```powershell
wsl --install
```

This single command:
- Enables the Windows Virtualization Layer
- Installs WSL2
- Downloads and installs **Ubuntu** as the default Linux distro

**Restart your PC** when it completes.

After restart, Ubuntu will launch automatically and ask you to create a Linux username and password. Set these — you'll need them when using `sudo`.

> If you already have WSL installed but on version 1, upgrade with:
> ```powershell
> wsl --set-default-version 2
> ```

---

## 3. Open WSL Shell

You have several options to open a WSL terminal:

| Method | Steps |
|---|---|
| **Fastest** | Press `Win + R` → type `wsl` → Enter |
| **Start Menu** | Search "Ubuntu" → click it |
| **Windows Terminal** | Open Windows Terminal → click `∨` dropdown → select Ubuntu |
| **PowerShell/CMD** | Type `wsl` and press Enter |

---

## 4. Download and Install AMPS Server

Inside your WSL shell, run the following commands:

```bash
# Navigate to home directory
cd ~

# Download AMPS (replace version with latest from https://crankuptheamps.com/releases/amps)
wget https://devnull.crankuptheamps.com/releases/amps/5.3.3.30/AMPS-5.3.3.30-Release-Linux.tar.gz

# Extract the archive
tar -zxf AMPS-5.3.3.30-Release-Linux.tar.gz
```

> **Check for the latest version** at: https://crankuptheamps.com/releases/amps
> Update the version number in the `wget` URL accordingly.

Verify the extraction succeeded:

```bash
ls ~/AMPS-5.3.3.30-Release-Linux/bin/
# You should see: ampServer  spark  (and other binaries)
```

---

## 5. Create AMPS Config with a Queue

AMPS requires an XML config file to start. The config below sets up:
- An **admin panel** on port `8085`
- A **TCP transport** on port `9007` for JSON messages (wrapped in `<Transports>` with `<Name>` and `<Protocol>` required)
- A **SOW (State of World) topic** that backs the queue
- A **Queue** named `orders-queue` (wrapped in `<Queues>`)

```bash
cat > ~/amps-config.xml << 'EOF'
<AMPSConfig>
  <Name>my-amps-server</Name>

  <!-- Admin web panel -->
  <Admin>
    <InetAddr>0.0.0.0:8085</InetAddr>
  </Admin>

  <!-- Transports: listens on all interfaces so Windows can reach it -->
  <Transports>
    <Transport>
      <Name>tcp-json</Name>
      <Type>tcp</Type>
      <InetAddr>0.0.0.0:9007</InetAddr>
      <Protocol>amps</Protocol>
      <MessageType>json</MessageType>
    </Transport>
  </Transports>

  <!-- SOW topic to store queue messages -->
  <SOW>
    <Topic>
      <Name>orders</Name>
      <MessageType>json</MessageType>
      <Key>/orderId</Key>
      <FileName>./sow/orders.sow</FileName>
    </Topic>
  </SOW>

  <!-- Queue backed by the SOW topic above -->
  <Queues>
    <Queue>
      <Name>orders-queue</Name>
      <Topic>orders</Topic>
      <MaxBacklog>100</MaxBacklog>
      <LeaseTimeout>30s</LeaseTimeout>
    </Queue>
  </Queues>

  <!-- Transaction log for durability -->
  <TransactionLog>
    <JournalDirectory>./journal</JournalDirectory>
    <Topic>
      <Name>orders</Name>
      <MessageType>json</MessageType>
    </Topic>
  </TransactionLog>

</AMPSConfig>
EOF
```

Create the required directories:

```bash
mkdir -p ~/sow ~/journal
```

---

## 6. Start AMPS Server

### Option A — Foreground (see logs in terminal)

```bash
~/AMPS-5.3.3.30-Release-Linux/bin/ampServer ~/amps-config.xml
```

Leave this terminal open. You'll see startup logs. Press `Ctrl+C` to stop.

### Option B — Background (keeps running after terminal closes)

```bash
nohup ~/AMPS-5.3.3.30-Release-Linux/bin/ampServer ~/amps-config.xml > ~/amps.log 2>&1 &
echo "AMPS PID: $!"
```

Check the log to confirm it started:

```bash
tail -f ~/amps.log
```

To stop the background server:

```bash
kill $(pgrep ampServer)
```

### Verify AMPS is running

Use the built-in `spark` utility to ping the server:

```bash
~/AMPS-5.3.3.30-Release-Linux/bin/spark ping -server localhost:9007/amps/json
```

You should see a successful ping response.

---

## 7. Find Your WSL2 IP

WSL2 runs on its own internal network. Your Spring Boot app on Windows must use the WSL2 IP, **not** `localhost`.

Open a **second WSL terminal** and run:

```bash
ip addr show eth0 | grep 'inet '
```

Example output:
```
inet 172.21.12.69/20 brd 172.21.15.255 scope global eth0
```

Your AMPS connection URL for the Spring Boot app will be:
```
tcp://172.21.12.69:9007/amps/json
```

> **Important:** This IP can change every time WSL2 or Windows restarts.
> Re-run this command after each reboot and update your `application.properties`.

---

## 8. Verify Connectivity from Windows

Before coding, confirm Windows can reach AMPS.

**Option A — Browser (admin panel)**

Open Chrome or Edge and go to:
```
http://172.21.12.69:8085
```
You should see the AMPS admin dashboard.

**Option B — PowerShell port test**

Open PowerShell on Windows and run:
```powershell
Test-NetConnection -ComputerName 172.21.12.69 -Port 9007
```
Look for:
```
TcpTestSucceeded : True
```

If it fails, check that Windows Firewall is not blocking the WSL2 subnet (usually `172.x.x.x`).

---

## 9. Spring Boot Integration

### 9.1 Add Maven Dependency

Add the AMPS Java client to your `pom.xml`:

```xml
<dependency>
    <groupId>com.crankuptheamps</groupId>
    <artifactId>amps-client</artifactId>
    <version>5.3.3.0</version>
</dependency>
```

> Check Maven Central for the latest version:
> https://mvnrepository.com/artifact/com.crankuptheamps/amps-client

### 9.2 application.properties

```properties
# Update this IP after every WSL2/Windows restart
amps.server.url=tcp://172.21.12.69:9007/amps/json
amps.queue.topic=orders
```

### 9.3 AMPS Configuration Bean

```java
// src/main/java/com/example/config/AmpsConfig.java

package com.example.config;

import com.crankuptheamps.client.Client;
import com.crankuptheamps.client.HAClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AmpsConfig {

    @Value("${amps.server.url}")
    private String ampsServerUrl;

    @Bean
    public HAClient ampsClient() throws Exception {
        HAClient client = new HAClient("springboot-app");
        client.connect(ampsServerUrl);
        client.logon();
        return client;
    }
}
```

> `HAClient` (High Availability Client) is recommended over the basic `Client`
> because it handles automatic reconnection if AMPS restarts.

### 9.4 Producer — Publish to Queue

```java
// src/main/java/com/example/producer/AmpsProducer.java

package com.example.producer;

import com.crankuptheamps.client.HAClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AmpsProducer {

    private final HAClient ampsClient;

    @Value("${amps.queue.topic}")
    private String topic;

    public AmpsProducer(HAClient ampsClient) {
        this.ampsClient = ampsClient;
    }

    public void publish(String jsonMessage) {
        try {
            ampsClient.publish(topic, jsonMessage);
            System.out.println("Published to AMPS queue: " + jsonMessage);
        } catch (Exception e) {
            System.err.println("Failed to publish: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
```

**Example usage in a REST controller:**

```java
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final AmpsProducer producer;

    public OrderController(AmpsProducer producer) {
        this.producer = producer;
    }

    @PostMapping
    public ResponseEntity<String> createOrder(@RequestBody String orderJson) {
        producer.publish(orderJson);
        return ResponseEntity.ok("Order queued successfully");
    }
}
```

### 9.5 Consumer — Subscribe to Queue

```java
// src/main/java/com/example/consumer/AmpsConsumer.java

package com.example.consumer;

import com.crankuptheamps.client.HAClient;
import com.crankuptheamps.client.Message;
import com.crankuptheamps.client.MessageHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class AmpsConsumer {

    private final HAClient ampsClient;

    @Value("${amps.queue.topic}")
    private String topic;

    public AmpsConsumer(HAClient ampsClient) {
        this.ampsClient = ampsClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startConsuming() {
        Thread consumerThread = new Thread(() -> {
            try {
                System.out.println("Starting AMPS queue consumer on topic: " + topic);
                ampsClient.subscribe(
                    (MessageHandler) message -> {
                        try {
                            String data = message.getData();
                            System.out.println("Received from AMPS queue: " + data);

                            // Process your message here
                            processMessage(data);

                            // Acknowledge the message so AMPS removes it from the queue
                            message.ack();

                        } catch (Exception e) {
                            System.err.println("Error processing message: " + e.getMessage());
                            // Not acking means AMPS will redeliver after lease timeout
                        }
                    },
                    topic,
                    5000  // timeout in ms
                );
            } catch (Exception e) {
                System.err.println("Consumer error: " + e.getMessage());
            }
        });
        consumerThread.setDaemon(true);
        consumerThread.start();
    }

    private void processMessage(String jsonData) {
        // Add your business logic here
        System.out.println("Processing: " + jsonData);
    }
}
```

### 9.6 Graceful Shutdown

Add a shutdown hook so the AMPS client disconnects cleanly:

```java
// src/main/java/com/example/config/AmpsShutdown.java

package com.example.config;

import com.crankuptheamps.client.HAClient;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

@Component
public class AmpsShutdown implements ApplicationListener<ContextClosedEvent> {

    private final HAClient ampsClient;

    public AmpsShutdown(HAClient ampsClient) {
        this.ampsClient = ampsClient;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        try {
            System.out.println("Disconnecting AMPS client...");
            ampsClient.disconnect();
        } catch (Exception e) {
            System.err.println("Error disconnecting AMPS: " + e.getMessage());
        }
    }
}
```

---

## 10. Common Issues and Fixes

| Problem | Cause | Fix |
|---|---|---|
| `Connection refused` from Spring Boot | Wrong IP or AMPS not running | Re-run `ip addr show eth0` in WSL; confirm AMPS is running |
| IP changed after restart | WSL2 gets a new IP on reboot | Re-run `ip addr show eth0` and update `application.properties` |
| AMPS stops when WSL terminal closes | Foreground process killed | Use `nohup` to run AMPS in background (see Step 6B) |
| `TcpTestSucceeded: False` in PowerShell | Windows Firewall blocking WSL subnet | Allow inbound connections from the `172.x.x.x` range in Windows Firewall |
| Config uses `127.0.0.1` not `0.0.0.0` | AMPS only binds to loopback | Ensure `<InetAddr>0.0.0.0:9007</InetAddr>` in config |
| `Topic needs to specify a MessageType` | Missing `<MessageType>` in TransactionLog topic | Add `<MessageType>json</MessageType>` inside the TransactionLog `<Topic>` block |
| `Transport is unrecognized` | Used `<Transport>` instead of `<Transports>` | Wrap transport in `<Transports>...</Transports>` |
| `Transport needs to specify a Name` | Missing `<Name>` inside `<Transport>` | Add `<Name>tcp-json</Name>` inside the `<Transport>` block |
| `Transport requires a Protocol` | Missing `<Protocol>` inside `<Transport>` | Add `<Protocol>amps</Protocol>` inside the `<Transport>` block |
| Messages not being removed from queue | Consumer not calling `message.ack()` | Always call `message.ack()` after successful processing |
| AMPS won't start — missing directories | SOW or journal dir doesn't exist | Run `mkdir -p ~/sow ~/journal` |

---

## 11. Quick Reference Cheat Sheet

```bash
# --- WSL SHELL COMMANDS ---

# Start WSL (from Windows PowerShell or Run dialog)
wsl

# Get WSL2 IP (run every time after restart)
ip addr show eth0 | grep 'inet '

# Start AMPS in foreground
~/AMPS-5.3.3.30-Release-Linux/bin/ampServer ~/amps-config.xml

# Start AMPS in background
nohup ~/AMPS-5.3.3.30-Release-Linux/bin/ampServer ~/amps-config.xml > ~/amps.log 2>&1 &

# Check AMPS logs (background mode)
tail -f ~/amps.log

# Ping AMPS to verify it's up
~/AMPS-5.3.3.30-Release-Linux/bin/spark ping -server localhost:9007/amps/json

# Stop background AMPS
kill $(pgrep ampServer)
```

```powershell
# --- WINDOWS POWERSHELL COMMANDS ---

# Test if AMPS port is reachable from Windows
Test-NetConnection -ComputerName 172.21.12.69 -Port 9007
```

```
# --- AMPS URLS ---

Admin Panel:     http://<WSL2-IP>:8085
Transport (app): tcp://<WSL2-IP>:9007/amps/json
```

---

## References

- [60East AMPS on Windows (official blog)](https://crankuptheamps.com/blog/amps-on-windows)
- [AMPS Java Client Docs](https://crankuptheamps.com/clients/amps-client-java)
- [AMPS Java Client — Using Queues](https://crankuptheamps.com/clients/amps-client-java/queues)
- [AMPS Releases Page](https://crankuptheamps.com/releases/amps)
- [Maven Central: amps-client](https://mvnrepository.com/artifact/com.crankuptheamps/amps-client)
