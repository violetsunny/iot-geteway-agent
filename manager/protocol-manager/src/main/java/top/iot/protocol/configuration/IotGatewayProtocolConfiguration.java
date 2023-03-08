package top.iot.protocol.configuration;

import lombok.extern.slf4j.Slf4j;
import top.iot.gateway.core.cluster.ClusterManager;
import top.iot.gateway.core.spi.ServiceContext;
import top.iot.gateway.supports.protocol.ServiceLoaderProtocolSupports;
import top.iot.gateway.supports.protocol.management.ClusterProtocolSupportManager;
import top.iot.gateway.supports.protocol.management.ProtocolSupportLoader;
import top.iot.gateway.supports.protocol.management.ProtocolSupportManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class IotGatewayProtocolConfiguration {
    @Bean
    public ProtocolSupportManager protocolSupportManager(ClusterManager clusterManager) {
        return new ClusterProtocolSupportManager(clusterManager);
    }

    @Bean(initMethod = "init")
    @ConditionalOnProperty(prefix = "top.iot.protocol.spi", name = "enabled", havingValue = "true")
    public ServiceLoaderProtocolSupports serviceLoaderProtocolSupports(ServiceContext serviceContext) {
        ServiceLoaderProtocolSupports supports = new ServiceLoaderProtocolSupports();
        supports.setServiceContext(serviceContext);
        return supports;
    }

    @Bean
    public LazyInitManagementProtocolSupports managementProtocolSupports(ProtocolSupportManager supportManager,
                                                                         ProtocolSupportLoader loader,
                                                                         ClusterManager clusterManager) {
        LazyInitManagementProtocolSupports supports = new LazyInitManagementProtocolSupports();
        supports.setClusterManager(clusterManager);
        supports.setManager(supportManager);
        supports.setLoader(loader);
        return supports;
    }

}
