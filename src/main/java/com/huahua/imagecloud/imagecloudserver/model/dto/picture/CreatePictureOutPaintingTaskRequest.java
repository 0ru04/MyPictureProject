package com.huahua.imagecloud.imagecloudserver.model.dto.picture;

import com.huahua.imagecloud.imagecloudserver.api.aliyunAI.model.CreateOutPaintingTaskRequest;
import lombok.Data;

import java.io.Serializable;

@Data
public class CreatePictureOutPaintingTaskRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 图片 id
     * */
    private Long pictureId;

    /**
     * 扩图提示词
     * */
    private String prompt;

    /**
     * 扩图参数
     * */
    private CreateOutPaintingTaskRequest.Parameters parameters;
}
