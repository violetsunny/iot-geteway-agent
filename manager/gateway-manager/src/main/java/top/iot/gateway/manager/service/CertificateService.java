package top.iot.gateway.manager.service;

import top.iot.gateway.manager.entity.CertificateEntity;
import top.iot.gateway.network.security.Certificate;
import top.iot.gateway.network.security.CertificateManager;
import top.iot.gateway.network.security.DefaultCertificate;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * @author wangzheng
 * @see
 * @since 1.0
 */
@Service
public class CertificateService
        extends GenericReactiveCrudService<CertificateEntity, String>
        implements CertificateManager {
    @Override
    public Mono<Certificate> getCertificate(String id) {
        return createQuery()
                .where(CertificateEntity::getId, id)
                .fetchOne()
                .map(entity -> {
                    DefaultCertificate defaultCertificate = new DefaultCertificate(entity.getId(), entity.getName());
                    return entity.getInstance().init(defaultCertificate, entity.getConfigs());
                });
    }
}
