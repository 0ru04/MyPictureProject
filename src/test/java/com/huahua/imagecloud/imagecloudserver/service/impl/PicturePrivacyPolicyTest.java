package com.huahua.imagecloud.imagecloudserver.service.impl;

import com.huahua.imagecloud.imagecloudserver.exception.BusinessException;
import com.huahua.imagecloud.imagecloudserver.model.entity.Picture;
import com.huahua.imagecloud.imagecloudserver.model.entity.User;
import com.huahua.imagecloud.imagecloudserver.model.enums.PictureReviewStatusEnum;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PicturePrivacyPolicyTest {

    private final PictureServiceImpl pictureService = new PictureServiceImpl();

    @Test
    void privatePictureBypassesPublicReview() {
        Picture picture = new Picture();
        picture.setSpaceId(42L);

        pictureService.fileReviewParams(picture, new User());

        assertThat(picture.getReviewStatus()).isEqualTo(PictureReviewStatusEnum.PASS.getValue());
        assertThat(picture.getReviewMessage()).isEqualTo("私有空间图片无需审核");
    }

    @Test
    void adminCannotOperateAnotherUsersPrivatePicture() {
        User admin = new User();
        admin.setId(1L);
        Picture picture = new Picture();
        picture.setUserId(2L);
        picture.setSpaceId(42L);

        assertThatThrownBy(() -> pictureService.checkPictureAuth(admin, picture))
                .isInstanceOf(BusinessException.class);
    }
}
