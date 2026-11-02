package com.huahua.imagecloud.imagecloudserver.service.impl;

import com.huahua.imagecloud.imagecloudserver.model.dto.picture.PictureUploadRequest;
import com.huahua.imagecloud.imagecloudserver.model.entity.Picture;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PictureUploadSpaceTest {

    @Test
    void updateUsesPersistedSpaceInsteadOfClientValue() {
        Picture existingPicture = new Picture();
        existingPicture.setSpaceId(42L);
        PictureUploadRequest request = new PictureUploadRequest();
        request.setSpaceId(999L);

        Long spaceId = PictureServiceImpl.resolveUploadSpaceId(existingPicture, request);

        assertThat(spaceId).isEqualTo(42L);
    }

    @Test
    void newPictureUsesRequestedSpace() {
        PictureUploadRequest request = new PictureUploadRequest();
        request.setSpaceId(42L);

        Long spaceId = PictureServiceImpl.resolveUploadSpaceId(null, request);

        assertThat(spaceId).isEqualTo(42L);
    }
}
