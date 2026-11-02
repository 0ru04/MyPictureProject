package com.huahua.imagecloud.imagecloudserver.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huahua.imagecloud.imagecloudserver.Constant.Constant;
import com.huahua.imagecloud.imagecloudserver.annotation.AuthCheck;
import com.huahua.imagecloud.imagecloudserver.common.BaseResponse;
import com.huahua.imagecloud.imagecloudserver.common.DeleteRequest;
import com.huahua.imagecloud.imagecloudserver.common.ResultUtils;
import com.huahua.imagecloud.imagecloudserver.exception.BusinessException;
import com.huahua.imagecloud.imagecloudserver.exception.ErrorCode;
import com.huahua.imagecloud.imagecloudserver.exception.ThrowUtils;
import com.huahua.imagecloud.imagecloudserver.model.dto.user.*;
import com.huahua.imagecloud.imagecloudserver.model.entity.User;
import com.huahua.imagecloud.imagecloudserver.model.vo.LoginUserVO;
import com.huahua.imagecloud.imagecloudserver.model.vo.UserVO;
import com.huahua.imagecloud.imagecloudserver.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


/**
 * @RestController将后端返回的值转换为 json 格式传递给前端
 * @RequestBody 将前端传递的json数据放在请求体里，解析json数据并绑定到方法参数上
 */
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;
    /**
     * 1.用户注册
     * */

    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        // 【防御性编程】如果前端没传参数或参数解析失败导致对象为 null，直接抛出“参数错误”异常，终止执行。
        ThrowUtils.throwIf(userRegisterRequest== null, ErrorCode.PARAMS_ERROR);

        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword=userRegisterRequest.getCheckPassword();
        long result=userService.userRegister(userAccount,userPassword,checkPassword);

        return ResultUtils.success(result);
    }

    /**
     * 2.用户登录 ---> 建立会话
     * HttpServletRequest request 接受来自前端的所有 http 请求
     * */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest== null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO=userService.userLogin(userAccount,userPassword,request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取当前登录页面的脱敏用户信息 LoginUserVO
     * */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request){

        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 用户（登录页面）注销
     * */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request){

        ThrowUtils.throwIf(request== null, ErrorCode.PARAMS_ERROR);
        boolean result=userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     *
     * 以下是管理员层面
     *
     * */

    /**
     * 管理员创建用户
     * */
    @PostMapping("/add")
    @AuthCheck(mustRole = Constant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest){
        ThrowUtils.throwIf(userAddRequest== null, ErrorCode.PARAMS_ERROR);
        User user=new  User();
        BeanUtils.copyProperties(userAddRequest,user);
        final String DEFAULT_PASSWORD="12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);

        //调用 Service 层将 user 对象写入数据库。成功返回 true，失败返回 false
        boolean result= userService.save(user);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROE);
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据id获取用户（仅管理员）
     * */
    @GetMapping("/get")
    @AuthCheck(mustRole = Constant.ADMIN_ROLE)
    public BaseResponse<User> getUser(@RequestParam Long id){
        ThrowUtils.throwIf(id<=0, ErrorCode.PARAMS_ERROR);
        User user=userService.getById(id);
        ThrowUtils.throwIf(user==null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }


    /**
     * 用户根据 id 查询，获取脱敏后的 UserVO（包装类）
     * */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(@RequestParam long id){
        // 1. 先调用内部方法获取完整的 User 实体对象
        BaseResponse<User> response = userService.getUserById(id);
        User user = response.getData();

        // 2. 将 User 实体转换为脱敏后的 UserVO 视图对象并返回
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 根据id 删除用户
     * */
    @PostMapping("/delete")
    @AuthCheck(mustRole = Constant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        // 1. 参数校验
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 2. 执行删除操作
        boolean result = userService.removeById(deleteRequest.getId());

        // 如果返回 false，说明影响行数为 0（即没删掉）
        if (!result) {
            // 这里抛出异常，或者返回特定的错误码
            throw new BusinessException(ErrorCode.OPERATION_ERROE, "删除失败：用户不存在或已删除");
        }
        // 3. 返回结果
        return ResultUtils.success(result);
    }

    /**
     * 更新用户(管理员)
     * */
    @PostMapping("/update")
    @AuthCheck(mustRole = Constant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        // 1. 参数校验：请求对象不能为空，且必须包含用户 ID
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 2. 创建待更新的实体对象
        User user = new User();

        // 3. 属性拷贝：将前端传来的参数复制到 User 实体中
        // 注意：这里只会拷贝非 null 的字段，避免覆盖数据库中的其他值
        BeanUtils.copyProperties(userUpdateRequest, user);

        // 4. 执行更新操作
        boolean result = userService.updateById(user);

        // 5. 结果校验：如果更新失败（比如 ID 不存在），抛出异常
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROE);

        // 6. 返回成功响应
        return ResultUtils.success(true);
    }

    /**
     * 分页获取用户封装列表（仅管理员）
     * POST /user/list/page/vo
     * Body: {"current": 1, "pageSize": 10, "userName": "张三", ...}
     *
     * @param userQueryRequest 查询请求参数（支持分页 + 条件过滤）
     * @return 分页结果，包含脱敏后的 UserVO 列表
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = Constant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        // 1. 参数校验：请求对象不能为空
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);

        // 2. 提取分页参数
        long current = userQueryRequest.getCurrent();   // 当前页码
        long pageSize = userQueryRequest.getPageSize(); // 每页条数

        // 3. 执行分页查询（带条件过滤）
        // Page <T> userpage = userService.page(page，wrapper)框架会自动拦截这个方法调用，解析参数，生成最终 SQL，执行查询，并把结果封装成 Page 对象。
        // page(E page, Wrapper<T> queryWrapper)
        Page<User> userPage = userService.page(
                new Page<>(current, pageSize),           // 第一份参数：分页对象
                userService.getQueryWrapper(userQueryRequest)
                // 第二个参数：调用userService手写的getQueryWrapper动态构建的查询条件
        );

        // 4. 创建新的分页对象用于存放 VO 数据（保留总记录数）
        // 创建个 Page<UserVO> 对象 只初始化分页元数据（当前页、每页大小、总记录数），此时列表数据 (records) 为空。
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());

        // 5. 将 User 实体列表转换为 UserVO 视图对象列表（脱敏处理）
        //负责把每一个 User 变成 UserVO 并打包成一个新的列表
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());

        // 6. 设置脱敏后的记录到新分页对象中
        userVOPage.setRecords(userVOList);

        // 7. 返回成功响应
        return ResultUtils.success(userVOPage);
    }



}
