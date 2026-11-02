package com.huahua.imagecloud.imagecloudserver.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.huahua.imagecloud.imagecloudserver.model.dto.picture.PictureQueryRequest;
import com.huahua.imagecloud.imagecloudserver.model.entity.Picture;
import com.huahua.imagecloud.imagecloudserver.service.impl.PictureServiceImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PictureQueryWrapperTest {

    private final PictureServiceImpl pictureService = new PictureServiceImpl();

    @Test
    void scopesPrivateSpaceQueryBySpaceId() {
        PictureQueryRequest request = new PictureQueryRequest();
        request.setSpaceId(42L);

        QueryWrapper<Picture> wrapper = pictureService.getQueryWrapper(request);

        assertThat(wrapper.getSqlSegment()).contains("space_id =");
        assertThat(wrapper.getParamNameValuePairs()).containsValue(42L);
        assertThat(wrapper.getSqlSegment()).doesNotContain("space_id IS NULL");
    }

    @Test
    void scopesPublicGalleryQueryToNullSpaceId() {
        PictureQueryRequest request = new PictureQueryRequest();
        request.setNullSpaceId(true);

        QueryWrapper<Picture> wrapper = pictureService.getQueryWrapper(request);

        assertThat(wrapper.getSqlSegment()).contains("space_id IS NULL");
    }
}
