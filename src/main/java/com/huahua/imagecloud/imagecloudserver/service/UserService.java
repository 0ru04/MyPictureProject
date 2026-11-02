package com.huahua.imagecloud.imagecloudserver.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.huahua.imagecloud.imagecloudserver.common.BaseResponse;
import com.huahua.imagecloud.imagecloudserver.model.dto.user.UserQueryRequest;
import com.huahua.imagecloud.imagecloudserver.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.huahua.imagecloud.imagecloudserver.model.vo.LoginUserVO;
import com.huahua.imagecloud.imagecloudserver.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 28710
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2026-03-08 10:21:27
*/
public interface UserService extends IService<User> {
    /**
     * 用户注册
     * */
    long userRegister(String userAccount,String userPassword,String checkPassword);

    /**
     * 获取加密后的密码
     * */
    String getEncryptPassword(String userPassword);

    /**
     * 用户登录
     * */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 将返回的 User 转换为 脱敏后的登录用户视图 （返回给前端用的）
     * */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取当前登录的用户
     * */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户（登录态）注销
     * */
    boolean userLogout(HttpServletRequest request);

    /**
     * 将返回的 User 转换为 脱敏后的个人用户视图 （返回给前端用的）
     * */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏后的用户信息列表
     * */
    List<UserVO> getUserVOList(List<User> userlist);

    /**
     * 获取查询条件
     */

    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    BaseResponse<User> getUserById(long id);

   boolean isadmin(User user);

    boolean deleteUser(long id);

}
