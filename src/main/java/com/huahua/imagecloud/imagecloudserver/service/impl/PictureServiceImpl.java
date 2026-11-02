package com.huahua.imagecloud.imagecloudserver.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huahua.imagecloud.imagecloudserver.api.aliyunAI.AliYunAiApi;
import com.huahua.imagecloud.imagecloudserver.api.aliyunAI.model.CreateOutPaintingTaskRequest;
import com.huahua.imagecloud.imagecloudserver.api.aliyunAI.model.CreateOutPaintingTaskResponse;
import com.huahua.imagecloud.imagecloudserver.exception.BusinessException;
import com.huahua.imagecloud.imagecloudserver.exception.ErrorCode;
import com.huahua.imagecloud.imagecloudserver.exception.ThrowUtils;
import com.huahua.imagecloud.imagecloudserver.manager.upload.FilePictureUpload;
import com.huahua.imagecloud.imagecloudserver.manager.upload.PictureUploadTemplate;
import com.huahua.imagecloud.imagecloudserver.manager.upload.UrlPictureUpload;
import com.huahua.imagecloud.imagecloudserver.model.dto.file.UploadPictureResult;
import com.huahua.imagecloud.imagecloudserver.model.dto.picture.*;
import com.huahua.imagecloud.imagecloudserver.model.entity.Picture;
import com.huahua.imagecloud.imagecloudserver.model.entity.Space;
import com.huahua.imagecloud.imagecloudserver.model.entity.User;
import com.huahua.imagecloud.imagecloudserver.model.enums.PictureReviewStatusEnum;
import com.huahua.imagecloud.imagecloudserver.model.vo.PictureVO;
import com.huahua.imagecloud.imagecloudserver.model.vo.UserVO;
import com.huahua.imagecloud.imagecloudserver.service.PictureService;
import com.huahua.imagecloud.imagecloudserver.mapper.PictureMapper;
import com.huahua.imagecloud.imagecloudserver.service.SpaceService;
import com.huahua.imagecloud.imagecloudserver.service.UserService;
import com.huahua.imagecloud.imagecloudserver.utils.ColorSimilarUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.jsoup.select.Elements;
import org.springframework.transaction.support.TransactionTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.imageio.ImageIO;


import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author 28710
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2026-03-08 16:25:40
*/
@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

    @Autowired
    private UserService userService;

    @Autowired
    private FilePictureUpload filePictureUpload;

    @Autowired
    private UrlPictureUpload urlPictureUpload;
    @Autowired
    private SpaceService spaceService;

    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private AliYunAiApi aliYunAiApi;

    @Autowired
    private RedissonClient redissonClient;


    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        // 在 PictureServiceImpl.uploadPicture 方法的最开头（校验登录之后）
        RRateLimiter rateLimiter = redissonClient.getRateLimiter("upload:" + loginUser.getId());
        rateLimiter.trySetRate(RateType.OVERALL, 5, 1, RateIntervalUnit.SECONDS); // 单用户5次/秒
        boolean pass = rateLimiter.tryAcquire(); // 尝试获取令牌
        if (!pass) {
            throw new BusinessException(ErrorCode.OPERATION_ERROE, "上传太频繁啦，1秒最多5次～");
        }

        // 检验空间是否存在
        Long pictureId = pictureUploadRequest == null ? null : pictureUploadRequest.getId();
        Picture existingPicture = pictureId == null ? null : this.getById(pictureId);
        Long spaceId = resolveUploadSpaceId(existingPicture, pictureUploadRequest);
        if(spaceId!=null){
            Space space=spaceService.getById(spaceId);
            ThrowUtils.throwIf(space==null,ErrorCode.NOT_FOUND_ERROR,"空间不存在");
            //必须是空间创建人才能上传
            if(!loginUser.getId().equals(space.getUserId())){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"没有空间权限");

            }
            if(space.getTotalCount()>=space.getMaxCount()){
                throw new BusinessException(ErrorCode.OPERATION_ERROE,"空间条数不足");
            }
            if (space.getTotalSize()>=space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROE,"空间大小不足");
            }
        }

        // 用于判断是新增还是更新图片
        // 如果是更新图片，需要校验图片是否存在
        if (pictureId != null) {
            Picture oldPicture = existingPicture;
            ThrowUtils.throwIf(oldPicture==null,ErrorCode.NOT_FOUND_ERROR,"图片不存在");

            // 仅本人或管理员可编辑
            if((oldPicture.getSpaceId() != null && !oldPicture.getUserId().equals(loginUser.getId()))
                    || (oldPicture.getSpaceId() == null
                    && !oldPicture.getUserId().equals(loginUser.getId())
                    && !userService.isadmin(loginUser))){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"仅图片本人或管理员编辑");
            }

            // 效验空间是否一致，没传spaceId 则复用原有图片的 spaceId
            if(spaceId==null){
                if(oldPicture.getSpaceId()!=null){
                    spaceId=oldPicture.getSpaceId();
                }
            }else{
                // 传了spaceId ，必须和原有图片一致
                if(ObjUtil.notEqual(oldPicture.getSpaceId(),spaceId)){
                    throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间id不一致");
                }
            }


        }

        // 上传图片，得到信息
        // 按照用户 id 划分目录 eg：public/888 ==> 按照空间划分目录
        String uploadPathPrefix ;
        if(spaceId==null){
            uploadPathPrefix= String.format("public/%s", loginUser.getId());
        }else{
            uploadPathPrefix= String.format("space/%s", spaceId);
        }
        //判断上传图片类型
        PictureUploadTemplate pictureUploadTemplate=filePictureUpload;
        if(inputSource instanceof String){
            pictureUploadTemplate= urlPictureUpload;
        }
        //引入图片解析包装类
        UploadPictureResult uploadPictureResult =pictureUploadTemplate .uploadPicture(inputSource, uploadPathPrefix);

        // 构造要入数据库的图片信息
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());

        //支持外层传来的图片名称
        String picName = uploadPictureResult.getPicName();
        if(pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        // 补充设置 spaceId
        picture.setSpaceId(spaceId);
        picture.setPicColor(uploadPictureResult.getPicColor());

        // 如果 pictureId 不为空，表示更新，否则是新增
        if (pictureId != null) {
            // 如果是更新，需要补充 id 和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }

        //补充审核参数
        this.fileReviewParams(picture,loginUser);

        // 开启事务
        Long finalSpaceId=spaceId;
        transactionTemplate.execute(status->{
            //saveOrUpdate 是 mybatis-plus 用来智能地判断该执行“新增”还是“更新”，并返回操作是否成功
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROE, "图片上传失败");
            if(finalSpaceId!=null){
                long originalSize = existingPicture == null || existingPicture.getPicSize() == null
                        ? 0L : existingPicture.getPicSize();
                long sizeDelta = picture.getPicSize() - originalSize;
                boolean update = spaceService.lambdaUpdate().eq(Space::getId,finalSpaceId)
                        .setSql("total_size=total_size+" + sizeDelta)
                        .setSql(pictureId == null, "total_count=total_count+1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROE, "额度更新失败");
            }
            return picture;
        });



        return PictureVO.objToVo(picture);
    }

    static Long resolveUploadSpaceId(Picture existingPicture, PictureUploadRequest pictureUploadRequest) {
        if (existingPicture != null) {
            return existingPicture.getSpaceId();
        }
        return pictureUploadRequest == null ? null : pictureUploadRequest.getSpaceId();
    }
    /**
     * 测试原图使用
     * */
    @Override
    public PictureVO uploadPictureNoCompress(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        // 检验空间是否存在
        Long spaceId = pictureUploadRequest == null ? null : pictureUploadRequest.getSpaceId();
        if(spaceId!=null){
            Space space=spaceService.getById(spaceId);
            ThrowUtils.throwIf(space==null,ErrorCode.NOT_FOUND_ERROR,"空间不存在");
            //必须是空间创建人才能上传
            if(!loginUser.getId().equals(space.getUserId())){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"没有空间权限");

            }
            if(space.getTotalCount()>=space.getMaxCount()){
                throw new BusinessException(ErrorCode.OPERATION_ERROE,"空间条数不足");
            }
            if (space.getTotalSize()>=space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROE,"空间大小不足");
            }
        }

        // 用于判断是新增还是更新图片
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }

        // 如果是更新图片，需要校验图片是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture==null,ErrorCode.NOT_FOUND_ERROR,"图片不存在");

            // 仅本人或管理员可编辑
            if((oldPicture.getSpaceId() != null && !oldPicture.getUserId().equals(loginUser.getId()))
                    || (oldPicture.getSpaceId() == null
                    && !oldPicture.getUserId().equals(loginUser.getId())
                    && !userService.isadmin(loginUser))){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }

            // 效验空间是否一致，没传spaceId 则复用原有图片的 spaceId
            if(spaceId==null){
                if(oldPicture.getSpaceId()!=null){
                    spaceId=oldPicture.getSpaceId();
                }
            }else{
                // 传了spaceId ，必须和原有图片一致
                if(ObjUtil.notEqual(oldPicture.getSpaceId(),spaceId)){
                    throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间id不一致");
                }
            }


        }

        // 上传图片，得到信息
        // 按照用户 id 划分目录 eg：public/888 ==> 按照空间划分目录
        String uploadPathPrefix ;
        if(spaceId==null){
            uploadPathPrefix= String.format("public/%s", loginUser.getId());
        }else{
            uploadPathPrefix= String.format("space/%s", spaceId);
        }
        //判断上传图片类型
        PictureUploadTemplate pictureUploadTemplate=filePictureUpload;
        if(inputSource instanceof String){
            pictureUploadTemplate= urlPictureUpload;
        }
        //引入图片解析包装类
        UploadPictureResult uploadPictureResult =pictureUploadTemplate .uploadPicture(inputSource, uploadPathPrefix,true);

        // 构造要入数据库的图片信息
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        picture.setOriginUrl(uploadPictureResult.getUrl());
        //支持外层传来的图片名称
        String picName = uploadPictureResult.getPicName();
        if(pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        // 补充设置 spaceId
        picture.setSpaceId(spaceId);
        picture.setPicColor(uploadPictureResult.getPicColor());

        // 如果 pictureId 不为空，表示更新，否则是新增
        if (pictureId != null) {
            // 如果是更新，需要补充 id 和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }

        //补充审核参数
        this.fileReviewParams(picture,loginUser);

        // 开启事务
        Long finalSpaceId=spaceId;
        transactionTemplate.execute(status->{
            //saveOrUpdate 是 mybatis-plus 用来智能地判断该执行“新增”还是“更新”，并返回操作是否成功
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROE, "图片上传失败");
            if(finalSpaceId!=null){
                boolean update = spaceService.lambdaUpdate().eq(Space::getId,finalSpaceId).
                        setSql("total_size=total_size+"+ picture.getPicSize())
                        .setSql("total_count=total_count+1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROE, "额度更新失败");
            }
            return picture;
        });



        return PictureVO.objToVo(picture);
    }
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();


        /***
         * 在 MyBatis-Plus 的 QueryWrapper 中，默认的连接符就是 and
         */

        // 从多字段中搜索
        //生成sql语句：AND (name LIKE '%searchText%' OR introduction LIKE '%searchText%')
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            //这里创建了一个 querrywrapper 的 qw 对象 ，qw 是独立的，它里面的 OR 只影响它自己内部，不会连接到主句去括号
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "user_id", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "pic_format", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "pic_width", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "pic_height", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "pic_size", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "pic_scale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "review_status", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewMessage), "review_message", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewer_id", reviewerId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "space_id", spaceId);
        queryWrapper.isNull(nullSpaceId, "space_id");
        // ge 大于等于
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "edit_time", startEditTime);
        // lt 小于
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "edit_time", endEditTime);
        /**
         *
         * 生成的SQL语句：
         * SELECT * FROM your_table
         * WHERE
         *   id = 1
         *
         *   AND tags LIKE ‘ % "Java" % ’
         *   AND tags LIKE ‘ %"Spring"% ’
         * */
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                //"\""：
                //外层的双引号：表示这是一个字符串。
                //里面的 \"：表示这个字符串的内容仅仅是一个 双引号字符 "。
                //结果：得到一个值为 " 的字符串。
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }


        // 排序
        /**
         *
         *
         * @param StrUtil.isNotEmpty(sortField)
         * 如果 sortField 是空的（比如用户没传排序字段），这个表达式返回 false
         *
         * @param sortOrder.equals("ascend")
         * true 为 升序，false 为降序
         *
         * @param sortField
         * 生成SQL：orderby xxx（sortField ）=xxx
         *
         * */
        String sortColumn = StrUtil.isBlank(sortField)
                ? null
                : StrUtil.toUnderlineCase(sortField);

        queryWrapper.orderBy(sortColumn != null, "ascend".equals(sortOrder), sortColumn);
        return queryWrapper;
    }

    /**
     *
     * 数据校验
     * */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR,"参数为空");
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");

        //用户如果传递了url，才校验
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }



    /**
     * 单条脱敏
     * */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * 分页获取图片封装
     *
     * 批量数据 脱敏返回
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {

        //从分页对象中提取出“当前页”的数据列表。
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        /**
         * 对 PictureVO 中的 User 类进行赋值
         *
         * */
        // 1. 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());

        // Map 中 有 1--user1, 2--user2
        //userService.listByIds(userIdSet) 输出 一个 List<User>
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息 PictureVO 中的 User 类
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();

            //先声明一个变量 user 用来存找到的用户对象，初始化为 null（表示还没找到）。
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                //.get(0) 取出列表里的第一个用户对象，赋值给 user
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {

        //1.效验参数
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        //不单单知识判空，还要确认审核的合法值
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum. PEVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2.判断图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(oldPicture.getSpaceId() != null, ErrorCode.NO_AUTH_ERROR,
                "私有空间图片无需管理员审核");
        // 3.效验审核是否重复
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }
        // 更新审核状态

        /**
         * 为什么要新建一个 Picture对象
         * 为什么不 this.updateById(oldPicture)
         * MP 会跟你传入的有值的字段去更新当前id这一行的内容，显然输入效率就不高
         * 应该只传入我们要注入的值 的对应的字段就好了
         * */
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROE);
    }

    /**
     * 填充审核参数
     *
     * */
    @Override
    public void fileReviewParams(Picture picture,User loginuser){
        if (picture.getSpaceId() != null) {
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewMessage("私有空间图片无需审核");
            return;
        }
        if(userService.isadmin(loginuser)){
            // 管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewTime(new Date());
            picture.setReviewerId(loginuser.getId());
            picture.setReviewMessage("管理员自动过审");
        }else {
            // 非管理员，创建或编辑都要改为待审核
            picture.setReviewStatus(PictureReviewStatusEnum.PEVIEWING.getValue());
        }
    }

    /**
     * 这是一个批量爬取并上传图片的方法，主要流程如下：
     * 1.参数校验：限制最多抓取 30 张图片。
     * 2.构造 Bing 图片搜索异步接口 URL。
     * 3.使用 Jsoup 请求页面并解析 HTML。
     * 4.定位包含图片的 <div class="dgControl"> 元素。
     * 5.提取所有 img.mimg 标签的 src 属性作为图片链接。
     * 6.清理 URL（去除查询参数），避免转义或冗余参数影响下载。
     * 7.调用单张上传方法 uploadPicture(...) 进行上传。
     * 8.记录成功/失败日志，计数达到目标后提前退出循环。
     * 9.返回实际成功上传的数量。
     * */
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {

        //1.效验参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多 30 条");
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        //名称前缀默认等于搜索关键词
        if(StrUtil.isBlank(namePrefix)){
            namePrefix = searchText;
        }
        // 2.要抓取的地址
        //https://cn.bing.com/images/async?q=%s&mmsync=1 可以被自动化抓取的结构化图片数据源。
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmsync=1", searchText);
        Document document;
        try {
            // 3.连接页面获取 HTML 文档
            document =  Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROE, "获取页面失败");
        }

        // 4.解析 html 内容
        // 定位包含图片的 <div class="dgControl"> 元素。
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROE, "获取元素失败");
        }

        // 提取所有 img.mimg 标签的 src 属性作为图片链接。

        //在刚才找到的那个大盒子（div）里面，把所有“类名是 mimg 的图片”都找出来，打包成一个列表
        Elements imgElementList = div.select("img.mimg");
        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过：{}", fileUrl);
                continue;
            }

            // 处理图片上传地址，防止出现转义问题
            //  substring 逻辑是非常经典且必要的“清洗”步骤。 它能帮你拿到最纯净的图片地址，避免下载到小图，也能避免重复下载。
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }

            // 上传图片 调用单张上传方法 uploadPicture(...) 进行上传。

            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setUrl(fileUrl);
            pictureUploadRequest.setPicName(namePrefix+ (uploadCount +1));
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功，id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }

            if (uploadCount >= count) {
                break;
            }
        }

        return uploadCount;
    }

    /**
     * 检查图片权限
     * */
    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        if(spaceId==null){
            // 如果是公共图库，不是图片本人和管理员都无限限
            if(!picture.getUserId().equals(loginUser.getId()) && !userService.isadmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"当前用户非图片本人或管理员");
        }}else{
            // 私人空间只能本人操作
            if (!picture.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"当前用户非本人操作");
            }
        }
    }

    /**
     * 删除图片
     * */
    @Override
    public void deletePicture(long pictureId, User loginUser){
        ThrowUtils.throwIf(pictureId<=0,ErrorCode.PARAMS_ERROR,"参数错误");
        ThrowUtils.throwIf(loginUser==null,ErrorCode.NO_AUTH_ERROR,"未登录");

        //判断照片是否存在
        Picture oldPicture=this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture==null,ErrorCode.NOT_FOUND_ERROR,"数据库中不存在该图片");

        //效验权限
        checkPictureAuth(loginUser,oldPicture);

        //开启事务
        transactionTemplate.execute(status->{
            //操作数据库
            boolean result=this.removeById(pictureId);
            ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROE);

            //释放额度
            Long spaceId=oldPicture.getSpaceId();
            if(spaceId!=null){
                boolean update=spaceService.lambdaUpdate()
                        .eq(Space::getId,spaceId)
                        .setSql("total_size=total_size-"+oldPicture.getPicSize())
                        .setSql("total_count=total_count-1")
                        .update();
                ThrowUtils.throwIf(!update,ErrorCode.OPERATION_ERROE,"额度更新失败");
            }
            return true;
        });

    }

    /**
     * 编辑图片
     * */
    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser){
        // 在此处将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());

        // 图片数据校验
        validPicture(picture);

        // 判断图片是否存在
        long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR,"图片不存在");

        // 权限效验
        checkPictureAuth(loginUser,oldPicture);

        //补充审核参数
        this.fileReviewParams(picture,loginUser);
        // 操作数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROE);
    }

    /**
     * 按目标颜色查询图片
     * */
    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser) {
        // 1.校验参数
        ThrowUtils.throwIf(spaceId==null || StrUtil.isBlank(picColor),ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser==null,ErrorCode.NO_AUTH_ERROR,"用户未登录");
        //2.校验空间权限
        Space space=spaceService.getById(spaceId);
        ThrowUtils.throwIf(space==null,ErrorCode.NO_AUTH_ERROR,"空间不存在");
        if(!loginUser.getId().equals(space.getUserId())){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"没有空间访问权限");
        }

        //3.查询该空间下的所有图片（必须有主色调）
        List<Picture> pictureList=this.lambdaQuery()
                .eq(Picture::getSpaceId,spaceId)
                .isNotNull(Picture::getPicColor)
                .list();

        //如果没有图片，直接返回列表
        if(CollUtil.isEmpty(pictureList)){
            return Collections.emptyList();
        }
        // 4.将目标颜色转为Color对象
        Color targetColor= Color.decode(picColor);
        //5. 计算相似度并实现升序排序
        List<Picture> sortedPictures=pictureList.stream().sorted(Comparator.comparingDouble(picture->{
            //提取图片主色调
            String hexColor=picture.getPicColor();
            // 没有主色调的图片放到最后（防御性编程）
            if(StrUtil.isBlank(hexColor)){
                return Double.MAX_VALUE;
            }
            Color pictureColor=Color.decode(hexColor);
           //把大数变成负数，从而变小，排在前面
            return -ColorSimilarUtils.calculateSimilarity( targetColor,pictureColor);
        })).limit(12).collect(Collectors.toList());

        //6.转换为PictureVO
        return sortedPictures.stream().map(PictureVO::objToVo).collect(Collectors.toList());
    }

    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest request, User loginUser) {
        //获取图片信息
        Long pictureId=request.getPictureId();
        Picture picture = this.getById(pictureId);
        if(picture==null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 权限效验
        checkPictureAuth(loginUser,picture);
        //构造请求参数
        CreateOutPaintingTaskRequest taskRequest=new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input=new CreateOutPaintingTaskRequest.Input();
        input.setFunction("expand");
        input.setPrompt(StrUtil.blankToDefault(
                request.getPrompt(), "扩展图片画面，保持原图主体、透视、光线和风格一致"));
        input.setBaseImageUrl(buildOutPaintingImageUrl(picture.getUrl()));
        taskRequest.setInput(input);
        taskRequest.setParameters(request.getParameters());

        //创建任务
        return aliYunAiApi.createOutPaintingTask(taskRequest);

    }

    /**
     * 阿里云扩图要求输入图片宽高都在 512~4096 像素之间。
     * 对缩略图做等比例放大并转换为 Base64，避免修改用户原有图片。
     */
    private String buildOutPaintingImageUrl(String imageUrl) {
        ThrowUtils.throwIf(StrUtil.isBlank(imageUrl), ErrorCode.PARAMS_ERROR, "图片地址不能为空");

        try {
            BufferedImage sourceImage = readOutPaintingImage(imageUrl);
            ThrowUtils.throwIf(sourceImage == null, ErrorCode.PARAMS_ERROR, "无法识别图片内容");

            int sourceWidth = sourceImage.getWidth();
            int sourceHeight = sourceImage.getHeight();
            if (sourceWidth >= 512 && sourceWidth <= 4096
                    && sourceHeight >= 512 && sourceHeight <= 4096) {
                return imageUrl;
            }

            double minScale = Math.max(512.0 / sourceWidth, 512.0 / sourceHeight);
            double maxScale = Math.min(4096.0 / sourceWidth, 4096.0 / sourceHeight);
            ThrowUtils.throwIf(minScale > maxScale, ErrorCode.PARAMS_ERROR,
                    "图片宽高比例过大，无法自动适配扩图接口");

            double scale = Math.max(minScale, Math.min(1.0, maxScale));
            int targetWidth = Math.max(512, Math.min(4096,
                    (int) Math.round(sourceWidth * scale)));
            int targetHeight = Math.max(512, Math.min(4096,
                    (int) Math.round(sourceHeight * scale)));

            BufferedImage targetImage = new BufferedImage(
                    targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = targetImage.createGraphics();
            try {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, targetWidth, targetHeight);
                graphics.setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.setRenderingHint(
                        RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);
                graphics.drawImage(sourceImage, 0, 0, targetWidth, targetHeight, null);
            } finally {
                graphics.dispose();
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            boolean written = ImageIO.write(targetImage, "jpg", outputStream);
            ThrowUtils.throwIf(!written, ErrorCode.OPERATION_ERROE, "扩图图片转换失败");
            return "data:image/jpeg;base64,"
                    + Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("扩图图片预处理失败，imageUrl = {}", imageUrl, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROE, "扩图图片预处理失败");
        }
    }

    /**
     * Java 8 原生 ImageIO 不支持 WebP，读取失败时借助 COS 图片处理转换成 JPG。
     */
    private BufferedImage readOutPaintingImage(String imageUrl) throws IOException {
        byte[] sourceBytes = HttpUtil.downloadBytes(imageUrl);
        BufferedImage sourceImage = ImageIO.read(new ByteArrayInputStream(sourceBytes));
        if (sourceImage != null) {
            return sourceImage;
        }

        String separator = imageUrl.contains("?") ? "&" : "?";
        String convertedUrl = imageUrl + separator + "imageMogr2/format/jpg";
        byte[] convertedBytes = HttpUtil.downloadBytes(convertedUrl);
        return ImageIO.read(new ByteArrayInputStream(convertedBytes));
    }

}




