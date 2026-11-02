package com.huahua.imagecloud.imagecloudserver.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huahua.imagecloud.imagecloudserver.api.aliyunAI.model.CreateOutPaintingTaskRequest;
import com.huahua.imagecloud.imagecloudserver.api.aliyunAI.model.CreateOutPaintingTaskResponse;
import com.huahua.imagecloud.imagecloudserver.model.dto.picture.*;
import com.huahua.imagecloud.imagecloudserver.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.huahua.imagecloud.imagecloudserver.model.entity.User;
import com.huahua.imagecloud.imagecloudserver.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 28710
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2026-03-08 16:25:40
*/
public interface PictureService extends IService<Picture> {
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) ;

    PictureVO uploadPictureNoCompress(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);
    void validPicture(Picture picture);
    void doPictureReview(PictureReviewRequest pictureReviewRequest,User loginUser);
    void fileReviewParams(Picture picture,User loginuser);
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);
    void checkPictureAuth(User loginUser,Picture picture);
    void deletePicture(long pictureId, User loginUser);
    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);
    List<PictureVO> searchPictureByColor(Long spaceId,String picColor,User loginUser);
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest request, User loginUser);



}
