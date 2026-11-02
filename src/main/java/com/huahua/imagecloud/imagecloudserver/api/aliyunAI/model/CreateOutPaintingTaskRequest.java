package com.huahua.imagecloud.imagecloudserver.api.aliyunAI.model;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建扩图任务请求类
 * */
@Data
public class CreateOutPaintingTaskRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模型名称，官方文档：wanx2.1-imageedit
     */
    private String model = "wanx2.1-imageedit";

    /**
     * 输入图像信息
     */
    private Input input;

    /**
     * 图像处理参数
     */
    private Parameters parameters;

    @Data
    public static class Input implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 图像编辑功能，扩图固定为 expand
         */
        private String function = "expand";

        /**
         * 必选，提示词，用于描述期望扩展出的画面内容
         */
        private String prompt;

        /**
         * 必选，输入图像的 URL 或 Base64 编码数据
         */
        @Alias("base_image_url")
        private String baseImageUrl;
    }

    @Data
    public static class Parameters implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 向上扩展比例，默认 1.0，范围 [1.0, 2.0]
         */
        @Alias("top_scale")
        private Float topScale = 1.0F;

        /**
         * 向下扩展比例，默认 1.0，范围 [1.0, 2.0]
         */
        @Alias("bottom_scale")
        private Float bottomScale = 1.0F;

        /**
         * 向左扩展比例，默认 1.0，范围 [1.0, 2.0]
         */
        @Alias("left_scale")
        private Float leftScale = 1.0F;

        /**
         * 向右扩展比例，默认 1.0，范围 [1.0, 2.0]
         */
        @Alias("right_scale")
        private Float rightScale = 1.0F;

        /**
         * 生成图片数量，默认 1，范围 [1, 4]
         */
        private Integer n = 1;

        /**
         * 随机数种子，用于控制生成内容的随机性
         */
        private Integer seed;

        /**
         * 是否添加 "AI 生成" 水印，默认 false
         */
        private Boolean watermark = false;
    }
}
