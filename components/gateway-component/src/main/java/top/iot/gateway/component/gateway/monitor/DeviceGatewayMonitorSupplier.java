package top.iot.gateway.component.gateway.monitor;

public interface DeviceGatewayMonitorSupplier {
      DeviceGatewayMonitor getDeviceGatewayMonitor(String id, String... tags);

}
