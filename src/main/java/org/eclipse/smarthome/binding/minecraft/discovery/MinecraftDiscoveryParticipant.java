package org.eclipse.smarthome.binding.minecraft.discovery;

import static org.eclipse.smarthome.binding.minecraft.MinecraftBindingConstants.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.jmdns.ServiceInfo;

import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.io.transport.mdns.discovery.MDNSDiscoveryParticipant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinecraftDiscoveryParticipant implements MDNSDiscoveryParticipant {

    private static final String MDNS_SERVICE_TYPE = "_minecraft-server._tcp.local.";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public Set<ThingTypeUID> getSupportedThingTypeUIDs() {
        return Collections.singleton(THING_TYPE_BRIDGE);
    }

    @Override
    public String getServiceType() {
        return MDNS_SERVICE_TYPE;
    }

    @Override
    public DiscoveryResult createResult(ServiceInfo service) {
        ThingUID uid = getThingUID(service);
        String url = service.getURLs().length > 0 ? service.getURLs()[0] : null;
        if (uid != null && url != null) {
            Map<String, Object> properties = new HashMap<>(2);
            properties.put(ENDPOINT, url);
            properties.put(URI, service.getPropertyString(URI));

            DiscoveryResult result = DiscoveryResultBuilder.create(uid).withProperties(properties)
                    .withLabel(service.getName()).withRepresentationProperty(ENDPOINT).build();
            return result;

        } else {
            return null;
        }
    }

    @Override
    public ThingUID getThingUID(ServiceInfo service) {
        int port = service.getPort();
        String host = service.getHostAddresses().length > 0 ? service.getHostAddresses()[0] : null;
        if (host != null) {
            String id = host.replaceAll("\\.", "_") + "__" + port;
            return new ThingUID(THING_TYPE_BRIDGE, id);
        } else {
            return null;
        }
    }

}
