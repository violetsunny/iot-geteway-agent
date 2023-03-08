package top.iot.gateway.agent.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.web.authorization.annotation.Authorize;
import top.iot.gateway.core.cluster.ClusterManager;
import top.iot.gateway.core.cluster.ServerNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RequestMapping("/cluster")
@RestController
@Tag(name = "集群管理")
public class ClusterInfoController {

    @Autowired
    private ClusterManager clusterManager;

    @GetMapping("/nodes")
    @Operation(summary = "获取集群节点")
    public Flux<ServerNode> getServerNodes() {
        return Flux.fromIterable(clusterManager.getHaManager().getAllNode());
    }

}
