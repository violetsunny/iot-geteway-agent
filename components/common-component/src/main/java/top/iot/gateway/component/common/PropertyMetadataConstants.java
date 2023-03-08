package top.iot.gateway.component.common;

import top.iot.gateway.core.message.DeviceMessage;
import top.iot.gateway.core.message.HeaderKey;
import top.iot.gateway.core.metadata.PropertyMetadata;

public interface PropertyMetadataConstants {

    /**
     * 属性来源
     */
    interface Source {
        //数据来源
        String id = "source";

        HeaderKey<String> headerKey = HeaderKey.of(id, null);

        //手动写值
        String manual = "manual";

        //规则,虚拟属性
        String rule = "rule";

        static boolean isManual(DeviceMessage message) {
            return message
                .getHeader(PropertyMetadataConstants.Source.headerKey)
                .map(PropertyMetadataConstants.Source.manual::equals)
                .orElse(false);
        }

        static void setManual(DeviceMessage message) {
            message.addHeader(headerKey, manual);
        }

        /**
         * 判断属性是否手动赋值
         *
         * @param metadata 属性物模型
         * @return 是否手动赋值
         */
        static boolean isManual(PropertyMetadata metadata) {
            return metadata.getExpand(id)
                           .map(manual::equals)
                           .orElse(false);
        }

        /**
         * 判断属性是否为规则
         *
         * @param metadata 物模型
         * @return 是否规则
         */
        static boolean isRule(PropertyMetadata metadata) {
            return  metadata
                .getExpand(id)
                .map(rule::equals)
                .orElse(false);
        }
    }

    /**
     * 属性读写模式
     */
    interface AccessMode {
        String id = "accessMode";

        //读
        String read = "r";
        //写
        String write = "w";
        //上报
        String report = "u";

        static boolean isRead(PropertyMetadata property) {
            return property
                .getExpand(id)
                .map(val -> val.toString().contains(read))
                .orElse(true);
        }

        static boolean isReport(PropertyMetadata property) {
            return property
                .getExpand(id)
                .map(val -> val.toString().contains(report))
                .orElse(true);
        }
    }
}
