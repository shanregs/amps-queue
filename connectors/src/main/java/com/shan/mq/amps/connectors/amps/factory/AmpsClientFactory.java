package com.shan.mq.amps.connectors.amps.factory;


import com.crankuptheamps.client.Client;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class AmpsClientFactory {

    public static Client createClient(String clientName, String host, int port) throws Exception {

        String uri = String.format("tcp://%s:%d/amps/json",host,port);

        log.info("Connecting to AMPS server: {}", uri);
        Client client = new Client(clientName);

        try {
            long startTime = System.currentTimeMillis();
            client.connect(uri);
            log.info("Connected to AMPS server: {} in {} ms",uri,System.currentTimeMillis() - startTime);
            client.logon();
            log.info("AMPS logon successful. Client Name: {}",clientName);
            return client;
        } catch (Exception ex) {
            log.error("Failed to connect to AMPS server. URI={}, Client={}",
                    uri,clientName,ex);
            throw ex;
        }
    }
}