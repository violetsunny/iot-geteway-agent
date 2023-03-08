//package top.iot.gateway.agent.web;
//
//import top.iot.gateway.network.integration.DeviceClient;
//import top.iot.gateway.network.integration.model.DeviceStateReq;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.collections.MapUtils;
//import org.apache.commons.lang3.StringUtils;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import javax.annotation.Resource;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
///**
// * @author ruanhongH
// */
//@Slf4j
//@RestController
//@RequestMapping("/webHook")
//public class JkWebHookController {
//
//    @Resource
//    private DeviceClient deviceClient;
//
//    @PostMapping("/monitor")
//    public String onWebHook(@RequestBody Map<String, String> param) {
//        log.info("webHook设备状态监听,传入参数：{}", param);
//        String action = MapUtils.getString(param, "action", "");
//        String clientId = MapUtils.getString(param, "clientid", "");
//        if (StringUtils.isEmpty(clientId)) {
//            return action;
//        }
//        if (!clientId.contains("D")) {
//            return action;
//        }
//        String[] clientIds = clientId.split("D");
//        DeviceStateReq deviceStateReq = new DeviceStateReq();
//        List<String> deviceIds = new ArrayList<>(1);
//        deviceIds.add(clientIds[clientIds.length - 1]);
//        deviceStateReq.setDeviceIds(deviceIds);
//        if ("client_connected".equals(action)) {
//            deviceStateReq.setFlag("1");
//            deviceStateReq.setState(1);
//            deviceClient.batchUpdateDeviceState(deviceStateReq);
//            deviceStateReq.setFlag("2");
//            deviceClient.batchUpdateDeviceState(deviceStateReq);
//            log.info("EMQX 连接接收参数：{}", param);
//
//        } else if ("client_disconnected".equals(action)) {
//            deviceStateReq.setFlag("1");
//            deviceStateReq.setState(-1);
//            deviceClient.batchUpdateDeviceState(deviceStateReq);
//            deviceStateReq.setFlag("2");
//            deviceClient.batchUpdateDeviceState(deviceStateReq);
//            log.info("EMQX 断开连接接收参数：{}", param);
//
//        } else {
//
//        }
//        return action;
//    }
//}
