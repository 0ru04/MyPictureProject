package com.huahua.imagecloud.imagecloudserver.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huahua.imagecloud.imagecloudserver.model.dto.picture.PictureQueryRequest;
import com.huahua.imagecloud.imagecloudserver.model.dto.space.SpaceAddRequest;
import com.huahua.imagecloud.imagecloudserver.model.dto.space.SpaceQueryRequest;
import com.huahua.imagecloud.imagecloudserver.model.entity.Picture;
import com.huahua.imagecloud.imagecloudserver.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.huahua.imagecloud.imagecloudserver.model.entity.User;
import com.huahua.imagecloud.imagecloudserver.model.vo.PictureVO;
import com.huahua.imagecloud.imagecloudserver.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author 28710
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2026-03-30 15:59:36
*/
public interface SpaceService extends IService<Space> {
    void validSpace(Space space,boolean add);
    void fillSpaceBySpaceLevel(Space space);
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);
    Long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);
}
