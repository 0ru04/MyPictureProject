package com.huahua.imagecloud.imagecloudserver.api.aliyunAI.model;

import cn.hutool.core.annotation.Alias;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 *
 * 查询任务响应类
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetOutPaintingTaskResponse {

    /**
     * 请求唯一标识
     */
    private String requestId;

    /**
     * 输出信息
     */
    private Output output;

    /**
     * 表示任务的输出信息
     */
    @Data
    public static class Output {

        /**
         * 任务 ID
         */
        private String taskId;

        /**
         * 任务状态
         * <ul>
         *     <li>PENDING: 排队中</li>
         *     <li>RUNNING: 处理中</li>
         *     <li>SUSPENDED: 挂起</li>
         *     <li>SUCCEDED: 执行成功</li>
         *     <li>FAILED: 执行失败</li>
         *     <li>UNKNOWN: 任务不存在或状态未知</li>
         * </ul>
         */
        private String taskStatus;

        /**
         * 提交时间
         * 格式：YYYY-MM-DD HH:mm:ss.SSS
         */
        private String submitTime;

        /**
         * 调度时间
         * 格式：YYYY-MM-DD HH:mm:ss.SSS
         */
        private String scheduledTime;

        /**
         * 结束时间
         * 格式：YYYY-MM-DD HH:mm:ss.SSS
         */
        private String endTime;

        /**
         * 任务失败时的错误码
         */
        private String code;

        /**
         * 任务失败时的错误信息
         */
        private String message;

        /**
         * 生成结果列表，wanx2.1-imageedit 使用 results[].url 返回图片地址
         */
        private List<Result> results;

        /**
         * 任务指标信息
         */
        @Alias("task_metrics")
        private TaskMetrics taskMetrics;

        /**
         * 兼容旧调用方保留的图片 URL，由 AliYunAiApi 从 results 中提取
         */
        private String outputImageUrl;
    }

    /**
     * 单张图片的生成结果
     */
    @Data
    public static class Result {

        /**
         * 输出图像 URL，有效期 24 小时
         */
        private String url;

        /**
         * 单张图片失败时的错误码
         */
        private String code;

        /**
         * 单张图片失败时的错误信息
         */
        private String message;
    }

    /**
     * 接口错误码
     * <p>接口成功请求不会返回该参数。</p>
     */
    private String code;

    /**
     * 接口错误信息
     * <p>接口成功请求不会返回该参数。</p>
     */
    private String message;

    /**
     * 任务指标信息
     */
    private TaskMetrics taskMetrics;

    /**
     * 输出信息统计
     */
    private Usage usage;

    /**
     * 表示任务的统计信息
     */
    @Data
    public static class TaskMetrics {

        /**
         * 总任务数
         */
        @Alias("TOTAL")
        private Integer total;

        /**
         * 成功任务数
         */
        @Alias("SUCCEEDED")
        private Integer succeeded;

        /**
         * 失败任务数
         */
        @Alias("FAILED")
        private Integer failed;
    }

    /**
     * 输出信息统计
     */
    @Data
    public static class Usage {

        /**
         * 模型成功生成图片的数量
         */
        @Alias("image_count")
        private Integer imageCount;
    }
}
