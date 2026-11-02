package com.huahua.imagecloud.imagecloudserver;

import com.huahua.imagecloud.imagecloudserver.api.aliyunAI.AliYunAiApi;
import com.huahua.imagecloud.imagecloudserver.controller.PictureController;
import com.huahua.imagecloud.imagecloudserver.model.entity.User;
import com.huahua.imagecloud.imagecloudserver.model.vo.PictureVO;
import com.huahua.imagecloud.imagecloudserver.service.PictureService;
import com.huahua.imagecloud.imagecloudserver.service.SpaceService;
import com.huahua.imagecloud.imagecloudserver.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PictureUploadBindingTest {

    @InjectMocks
    private PictureController pictureController;

    private MockMvc mockMvc;

    @Mock
    private PictureService pictureService;

    @Mock
    private UserService userService;

    @Mock
    private SpaceService spaceService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private AliYunAiApi aliYunAiApi;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(pictureController).build();
    }

    @Test
    void bindsFileAndPictureUploadRequest() throws Exception {
        User loginUser = new User();
        loginUser.setId(1L);
        when(userService.getLoginUser(any())).thenReturn(loginUser);
        when(pictureService.uploadPicture(any(), any(), any())).thenReturn(new PictureVO());

        MockMultipartFile file = new MockMultipartFile(
                "file", "probe.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/picture/upload")
                        .file(file)
                        .param("picName", "probe"))
                .andExpect(status().isOk());
    }

    @Test
    void bindsJsonPictureUrlRequest() throws Exception {
        User loginUser = new User();
        loginUser.setId(1L);
        when(userService.getLoginUser(any())).thenReturn(loginUser);
        when(pictureService.uploadPicture(any(), any(), any())).thenReturn(new PictureVO());

        mockMvc.perform(post("/picture/upload/url")
                        .contentType(APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/a.jpg\"}"))
                .andExpect(status().isOk());

        verify(pictureService).uploadPicture(
                eq("https://example.com/a.jpg"), any(), any());
    }

    @Test
    void bindsMultipartPictureUrlRequest() throws Exception {
        User loginUser = new User();
        loginUser.setId(1L);
        when(userService.getLoginUser(any())).thenReturn(loginUser);
        when(pictureService.uploadPicture(any(), any(), any())).thenReturn(new PictureVO());

        mockMvc.perform(multipart("/picture/upload/url")
                        .param("url", "https://example.com/b.jpg"))
                .andExpect(status().isOk());

        verify(pictureService).uploadPicture(
                eq("https://example.com/b.jpg"), any(), any());
    }
}
