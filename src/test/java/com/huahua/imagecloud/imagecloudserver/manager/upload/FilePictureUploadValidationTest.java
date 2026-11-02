package com.huahua.imagecloud.imagecloudserver.manager.upload;

import com.huahua.imagecloud.imagecloudserver.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FilePictureUploadValidationTest {

    private final TestFilePictureUpload upload = new TestFilePictureUpload();

    @Test
    void acceptsFilesUpToTenMegabytes() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.JPG", "image/jpeg", new byte[5 * 1024 * 1024]);

        assertDoesNotThrow(() -> upload.validate(file));
    }

    @Test
    void acceptsWebpFiles() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.webp", "image/webp", new byte[1024]);

        assertDoesNotThrow(() -> upload.validate(file));
    }

    @Test
    void rejectsFilesLargerThanTenMegabytes() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[11 * 1024 * 1024]);

        assertThrows(BusinessException.class, () -> upload.validate(file));
    }

    private static class TestFilePictureUpload extends FilePictureUpload {
        void validate(Object inputSource) {
            super.validPicture(inputSource);
        }
    }
}
