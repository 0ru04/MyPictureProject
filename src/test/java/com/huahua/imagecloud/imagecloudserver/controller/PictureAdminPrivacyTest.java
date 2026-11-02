package com.huahua.imagecloud.imagecloudserver.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huahua.imagecloud.imagecloudserver.exception.BusinessException;
import com.huahua.imagecloud.imagecloudserver.model.dto.picture.PictureQueryRequest;
import com.huahua.imagecloud.imagecloudserver.model.dto.picture.PictureUpdateRequest;
import com.huahua.imagecloud.imagecloudserver.model.entity.Picture;
import com.huahua.imagecloud.imagecloudserver.service.PictureService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PictureAdminPrivacyTest {

    @Mock
    private PictureService pictureService;

    @InjectMocks
    private PictureController pictureController;

    @Test
    void adminPictureListIsRestrictedToPublicGallery() {
        PictureQueryRequest request = new PictureQueryRequest();
        request.setSpaceId(42L);
        when(pictureService.page(any(), any())).thenReturn(new Page<>());

        pictureController.listPictureByPage(request);

        assertThat(request.getSpaceId()).isNull();
        assertThat(request.isNullSpaceId()).isTrue();
    }

    @Test
    void adminCannotUpdatePrivatePicture() {
        Picture privatePicture = new Picture();
        privatePicture.setSpaceId(42L);
        when(pictureService.getById(100L)).thenReturn(privatePicture);
        PictureUpdateRequest request = new PictureUpdateRequest();
        request.setId(100L);

        assertThatThrownBy(() -> pictureController.updatePicture(request, null))
                .isInstanceOf(BusinessException.class);
    }
}
