package top.iot.gateway.network.security;

import reactor.core.publisher.Mono;

/**
 * 证书管理接口
 *
 * @author hanyl
 */
public interface CertificateManager {

    Mono<Certificate> getCertificate(String id);

}
