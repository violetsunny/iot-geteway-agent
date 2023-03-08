/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.iot.gateway.component.elasticsearch.index;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * es基础
 *
 * @author kanglele
 * @version $Id: EsBaseData, v 0.1 2023/1/13 15:01 kanglele Exp $
 */
@Data
public class EsBaseData implements Serializable {

    @Schema(description = "ID")
    private String id;

}
