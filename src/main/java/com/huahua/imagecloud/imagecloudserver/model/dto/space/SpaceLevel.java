package com.huahua.imagecloud.imagecloudserver.model.dto.space;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceLevel {

    private int value;
    private String text;
    private long maxCount;
    private long maxSize;


}
