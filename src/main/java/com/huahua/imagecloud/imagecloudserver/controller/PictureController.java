package com.huahua.imagecloud.imagecloudserver.controller;


import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.huahua.imagecloud.imagecloudserver.Constant.Constant;
import com.huahua.imagecloud.imagecloudserver.annotation.AuthCheck;
import com.huahua.imagecloud.imagecloudserver.api.aliyunAI.AliYunAiApi;
import com.huahua.imagecloud.imagecloudserver.api.aliyunAI.model.CreateOutPaintingTaskResponse;
import com.huahua.imagecloud.imagecloudserver.api.aliyunAI.model.GetOutPaintingTaskResponse;
import com.huahua.imagecloud.imagecloudserver.api.imagesearch.ImageSearchApiFacade;
import com.huahua.imagecloud.imagecloudserver.api.imagesearch.model.ImageSearchResult;
import com.huahua.imagecloud.imagecloudserver.common.BaseResponse;
import com.huahua.imagecloud.imagecloudserver.common.DeleteRequest;
import com.huahua.imagecloud.imagecloudserver.common.ResultUtils;
import com.huahua.imagecloud.imagecloudserver.exception.BusinessException;
import com.huahua.imagecloud.imagecloudserver.exception.ErrorCode;
import com.huahua.imagecloud.imagecloudserver.exception.ThrowUtils;
import com.huahua.imagecloud.imagecloudserver.model.dto.picture.*;
import com.huahua.imagecloud.imagecloudserver.model.entity.Picture;
import com.huahua.imagecloud.imagecloudserver.model.entity.Space;
import com.huahua.imagecloud.imagecloudserver.model.entity.User;
import com.huahua.imagecloud.imagecloudserver.model.enums.PictureReviewStatusEnum;
import com.huahua.imagecloud.imagecloudserver.model.vo.PictureTagCategory;
import com.huahua.imagecloud.imagecloudserver.model.vo.PictureVO;
import com.huahua.imagecloud.imagecloudserver.service.PictureService;
import com.huahua.imagecloud.imagecloudserver.service.SpaceService;
import com.huahua.imagecloud.imagecloudserver.service.UserService;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.statement.select.KSQLWindow;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("/picture")
@Slf4j
public class PictureController {

    @Autowired
    private PictureService pictureService;

    @Autowired
    private UserService userService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 构建本地缓存
     * */
    private final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1024)        // 【初始容量】
            .maximumSize(10000L)          // 【最大容量】
            .expireAfterWrite(5L, TimeUnit.MINUTES) // 【过期策略，5 分钟后过期】
            .build();                     // 【构建缓存对象】
    @Autowired
    private SpaceService spaceService;
    @Autowired
    private AliYunAiApi aliYunAiApi;


    /**
     * 上传图片（可重新上传）
     *
     * @param multipartFile
     * 找到http里面name为file的图片信息然后封装成一个multipartFile对象存储上传过来的图片信息
     *
     * @param pictureUploadRequest
     * 上传 ID负责告诉系统这张图该“挂”在谁身上
     *
     * @param request
     * spring MVC 自动注入 获取 session
     */
    @PostMapping("/upload")
    //@AuthCheck(mustRole = Constant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {

        // 获取当前用户的登录状态
        User loginUser = userService.getLoginUser(request);

        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }
    /**
     * 临时压测用：无 webp 压缩 / 无缩略图的纯上传接口
     * 测完直接删！
     */
    @PostMapping("/upload/no-compress")
    public BaseResponse<PictureVO> uploadPictureNoCompress(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        // 直接调用无压缩上传逻辑（这里用反射或临时写个简单 service 方法都可以）
        // 简单做法：直接把 PictureUploadTemplate.uploadPicture 里的压缩开关关掉再调
        PictureVO pictureVO = pictureService.uploadPictureNoCompress(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }
/**
 * 通过 url 上传图片
 * */
    @PostMapping(value = "/upload/url", consumes = MediaType.APPLICATION_JSON_VALUE)
    //@AuthCheck(mustRole = Constant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPictureByUrl(
            @RequestBody PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        return doUploadPictureByUrl(pictureUploadRequest, request);
    }

    @PostMapping(value = "/upload/url",
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE,
                    MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public BaseResponse<PictureVO> uploadPictureByUrlForm(
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        return doUploadPictureByUrl(pictureUploadRequest, request);
    }

    private BaseResponse<PictureVO> doUploadPictureByUrl(
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadRequest == null
                        || StrUtil.isBlank(pictureUploadRequest.getUrl()),
                ErrorCode.PARAMS_ERROR, "图片地址不能为空");
        // 获取当前用户的登录状态
        User loginUser = userService.getLoginUser(request);
        String url = pictureUploadRequest.getUrl();
        PictureVO pictureVO = pictureService.uploadPicture(url, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数错误");
        }
        User loginUser = userService.getLoginUser(request);
        pictureService.deletePicture(deleteRequest.getId(), loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 更新图片（仅管理员可用）
     * 无需脱敏
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = Constant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest,HttpServletRequest request) {
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 数据校验
        pictureService.validPicture(picture);

        // 判断是否存在
        long id = pictureUpdateRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(oldPicture.getSpaceId() != null, ErrorCode.NO_AUTH_ERROR,
                "私有空间图片不允许管理员操作");

        //补充审核参数
        User loginUser = userService.getLoginUser(request);
        pictureService.fileReviewParams(picture,loginUser);

        // 操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROE);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取图片（封装类）
     * 给用户看的 需要脱敏
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 空间权限效验
        Long spaceId=picture.getSpaceId();
        // 如果图片有spaceId
        if(spaceId !=null){
            User loginUser = userService.getLoginUser(request);
            pictureService.checkPictureAuth(loginUser,picture);
        }
        // 获取封装类
        return ResultUtils.success(pictureService.getPictureVO(picture, request));
    }

    /**
     * 分页获取图片列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = Constant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        pictureQueryRequest.setSpaceId(null);
        pictureQueryRequest.setNullSpaceId(true);
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页获取图片列表（封装类）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        applyPictureSpaceScope(pictureQueryRequest, request);

        // 查询数据库
        // Page<Picture>包含“数据”和“元数据”的完整对象。
        // new Page<>(current, size) 返回值是Page<Picture>
        // pictureService.page() 找出符合“第二个参数条件”的数据，然后只取出“第一个参数指定那一页”的数据，最后打包成 Page 对象返回。
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        // 获取封装类
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
    }


    /**
     * 分页获取图片列表（封装类）
     */

    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {

        //加上这一行，就能看到请求参数了
        log.info("=== 接收到带缓存的分页请求: current={}, size={}",
                pictureQueryRequest.getCurrent(),
                pictureQueryRequest.getPageSize());
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        applyPictureSpaceScope(pictureQueryRequest, request);
        //构建缓存key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String cacheKey="pictureList:v2:"+hashKey;
        // 1.从本地缓存中查询
         String cachedValue=LOCAL_CACHE.getIfPresent(cacheKey);

        //缓存命中，返回结果
        if (cachedValue != null) {
            Page<PictureVO> cachedPage=JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachedPage);
        }

        // 2.本地缓存失效，查询redis分布式缓存
        ValueOperations<String,String> valueOps=stringRedisTemplate.opsForValue();
        cachedValue=valueOps.get(cacheKey);

        //如果命中 Redis ，存入本地缓存并返回
        if (cachedValue != null) {
            LOCAL_CACHE.put(cacheKey,cachedValue);
            Page<PictureVO> cachedPage=JSONUtil.toBean(cachedValue, Page.class);
            return  ResultUtils.success(cachedPage);
        }
        //3.如果 redis 也未命中，则查询数据库，并将结果写入redis和本地缓存
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        //获取封装类
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);

        //4.更新缓存
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        // 更新本地缓存
        LOCAL_CACHE.put(cacheKey,cacheValue);
        // 更新 redis 缓存，设置过期时间为 5 分钟
        // 5-10分钟的随机过期时间，防止雪崩
        int cachExpireTime=300+ RandomUtil.randomInt(0,300);
        // 更新 redis 缓存，设置过期时间为 5 - 10 分钟
        valueOps.set(cacheKey,cacheValue,cachExpireTime, TimeUnit.SECONDS);
        // 获取封装类
        return ResultUtils.success( pictureVOPage);
    }

    private void applyPictureSpaceScope(PictureQueryRequest pictureQueryRequest,
                                        HttpServletRequest request) {
        Long spaceId = pictureQueryRequest.getSpaceId();
        if (spaceId == null) {
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
            return;
        }

        pictureQueryRequest.setNullSpaceId(false);
        User loginUser = userService.getLoginUser(request);
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (!loginUser.getId().equals(space.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
        }
    }


    /**
     * 编辑图片（给用户使用）
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数错误");
        }
        User loginUser = userService.getLoginUser(request);
        pictureService.editPicture(pictureEditRequest, loginUser);
        return ResultUtils.success(true);
    }

    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    @PostMapping("/review")
    @AuthCheck(mustRole = Constant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest,HttpServletRequest request){
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR,"参数错误");
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);


    }

    @PostMapping("/upload/catch")
    @AuthCheck(mustRole = Constant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(
            @RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
           HttpServletRequest request){
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR,"参数错误");
        User loginUser = userService.getLoginUser(request);
        int uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(uploadCount);
    }

    /**
     * 私有空间 以图搜图
     * */
    @PostMapping("/search/picture")
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(
            @RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest,
            HttpServletRequest request){
        ThrowUtils.throwIf(searchPictureByPictureRequest==null,ErrorCode.PARAMS_ERROR);
        Long pictureId = searchPictureByPictureRequest.getPictureId();
        ThrowUtils.throwIf(pictureId==null|| pictureId<=0,ErrorCode.PARAMS_ERROR);
        Picture oldPicture=pictureService.getById(pictureId);
        ThrowUtils.throwIf(oldPicture==null,ErrorCode.NOT_FOUND_ERROR,"没找到该图片");
        User loginUser = userService.getLoginUser(request);
        pictureService.checkPictureAuth(loginUser, oldPicture);
        List<ImageSearchResult> resultList= ImageSearchApiFacade.searchImage(oldPicture.getUrl());
        return ResultUtils.success(resultList);
    }


    /**
     * 私有空间 按目标颜色搜图
     * */
    @PostMapping("/search/color")
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureByColorRequest searchPictureByColorRequest ,HttpServletRequest httpServletRequest){
        ThrowUtils.throwIf(searchPictureByColorRequest==null,ErrorCode.PARAMS_ERROR,"用户未登录");
        Long spaceId = searchPictureByColorRequest.getSpaceId();
        String picColor= searchPictureByColorRequest.getPicColor();
        User loginUser = userService.getLoginUser(httpServletRequest);
        List<PictureVO> result=pictureService.searchPictureByColor(spaceId,picColor,loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 创建 AI 扩图任务
     * */
    @PostMapping("/out_picture/create_task")
    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(
            @RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
            HttpServletRequest httpServletRequest){
        if(createPictureOutPaintingTaskRequest == null|| createPictureOutPaintingTaskRequest.getPictureId()==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpServletRequest);
        CreateOutPaintingTaskResponse response=pictureService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest,loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 查询 AI 扩图任务
     * */
    @GetMapping("/out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId){
        ThrowUtils.throwIf(taskId==null,ErrorCode.PARAMS_ERROR);
        GetOutPaintingTaskResponse task=aliYunAiApi.getOutPaintingTask(taskId);
        return ResultUtils.success(task);
    }


















}
