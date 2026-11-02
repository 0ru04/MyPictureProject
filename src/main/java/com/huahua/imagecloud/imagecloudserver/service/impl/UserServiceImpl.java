package com.huahua.imagecloud.imagecloudserver.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huahua.imagecloud.imagecloudserver.Constant.Constant;
import com.huahua.imagecloud.imagecloudserver.common.BaseResponse;
import com.huahua.imagecloud.imagecloudserver.common.ResultUtils;
import com.huahua.imagecloud.imagecloudserver.exception.BusinessException;
import com.huahua.imagecloud.imagecloudserver.exception.ErrorCode;
import com.huahua.imagecloud.imagecloudserver.model.dto.user.UserQueryRequest;
import com.huahua.imagecloud.imagecloudserver.model.entity.User;
import com.huahua.imagecloud.imagecloudserver.model.enums.UserRoleEnums;
import com.huahua.imagecloud.imagecloudserver.model.vo.LoginUserVO;
import com.huahua.imagecloud.imagecloudserver.model.vo.UserVO;
import com.huahua.imagecloud.imagecloudserver.service.UserService;
import com.huahua.imagecloud.imagecloudserver.mapper.UserMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {

    /**
     * 用户注册
     * */

    @Override
    public long userRegister(String userAccount,String userPassword,String checkPassword){
        //1.数据校验
        //1.1 参数是否为空
        if(StrUtil.hasBlank(userAccount,userPassword,checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        }

        //1.2账号长度是否过短
        if(userAccount.length()<4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账号过短");
        }

        //1.3密码长度是否过短
        if(userPassword.length()<8||checkPassword.length()<8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户密码过短");
        }

        //1.4 输入的密码与核对密码是否一致
        if(!userPassword.equals(checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"两次输入密码不一致");
        }

        //2.账号查重

        //2.1 SQL构建器，自动拼接 where user_account = ?
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_account",userAccount);

        //2.2 baseMapper这个动态代理对象拦截了UserMapper（继承了BaseMapper）里的selectCount方法
        //，将上面的queryWrapper拼接完整的SQL语句，由JDBC将SQL和参数传递给数据库，并返回。
        long count = this.baseMapper.selectCount(queryWrapper);
        if(count>0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"该账号已被注册过");
        }

        //3.密码加密
        String encryptPassword = getEncryptPassword(userPassword);

        //4.插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword); //先将密码加密再插入数据表
        user.setUserName("无名");
        user.setUserRole(UserRoleEnums.USER.getValue()); //USER是一个枚举对象，没办法塞进数据库里

//        IService 接口（及其实现类 ServiceImpl）提供的快捷方法，往数据库插入数据
        //save（）= 往数据库里加一条新数据 = insert
        boolean saveResult = this.save(user);

        if(!saveResult){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"注册失败，数据库错误");
        }
        return user.getId();
    }

    /**
     * 用户登录
     **/
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request){
        //1.数据校验
        if(StrUtil.hasBlank(userAccount,userPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        }

        if(userAccount.length()<4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账号错误");
        }

        if(userPassword.length()<8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户不存在或密码错误");
        }

        //2.密码加密
        String encryptPassword = getEncryptPassword(userPassword);
        //查询用户是否存在
        QueryWrapper<User> querryWrapper = new QueryWrapper<>();
        querryWrapper.eq("user_account",userAccount);
        querryWrapper.eq("user_password",encryptPassword);
        User user = this.baseMapper.selectOne(querryWrapper);

        //用户不存在
        if(user==null){
            log.error("user login failed,userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"用户不存在");
        }

        //3.记录用户的登录态

        /**
         * Web 开发中实现“保持登录状态”的核心动作
         * request.getSession() 创建存储柜，
         * setAttribute("user-login-status",user) 给存储柜贴上key（"user-login-status"）标签，并且存入user对象
         */

        // 把当前登录的用户信息“存”到服务器的会话（Session）中，标记该用户已登录
        request.getSession().setAttribute(Constant.USER_LOGIN_STATUS,user);

        return this.getLoginUserVO(user);
        //return getLoginUserVO(user); this当前类的对象(@Service)
    }

    /**
     * 加密用户输入的密码
     * */
    @Override
    public String getEncryptPassword(String userPassword){

        //盐值，混淆密码
        final String SALT="LIUZEHUAHBO";
        //将输入的二进制通过 MD5 算法计算出一个唯一的指纹，
        //  并将其转换为一串人类可读的十六进制文本字符串
        return DigestUtils.md5DigestAsHex((SALT+userPassword).getBytes());
    }

    /**
     * 下面 一二 对应 Controller 层的第三层 ：获取当前登录页面的脱敏用户信息 LoginUserVO
     *
     * */

    /**
     *    将user对象脱敏，转换为用户视图 LoginUserVO 返回给前端
     */

    @Override
    public LoginUserVO getLoginUserVO(User user){
        if(user==null){
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        //把第一个参数的值传给第二个参数
        BeanUtils.copyProperties(user,loginUserVO);
        return loginUserVO;
    }


    /**
     * 获取当前登录状态
     * */
    @Override
    public User getLoginUser(HttpServletRequest request){
        //先判断是否已登录
        //这个 "user-login-status" 是你在用户成功登录时（通常在 userLogin 方法里）存进去的
        Object obj = request.getSession().getAttribute(Constant.USER_LOGIN_STATUS);
        User currentUser = (User)obj;

        //这里做了两层防御
        //1.如果 obj 是 null（说明 Session 里根本没这个东西），强制转换后 currentUser 也是 null。
        //2.如果 User 对象存在，但连最重要的 id 都没有，说明这个用户数据是无效的。
        if(currentUser==null || currentUser.getId()==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR,"未登录或用户不存在");
        }

        /**
         * 从数据库查询 防止读取脏数据，让查询更加真实
         * long userId = currentUser.getId();
         * currentUser = this.getById(userId);
         * if (currentUser == null) {
         *   throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
         * }
         */

        return currentUser;
    }

    /**
     * 用户（登录态）注销
     * */
    @Override
    public boolean userLogout(HttpServletRequest request){
        //先判断是否已登录
        Object userobj = request.getSession().getAttribute(Constant.USER_LOGIN_STATUS);
        if(userobj==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"未登录");
        }

        //移除登录态
        request.getSession().removeAttribute(Constant.USER_LOGIN_STATUS);
        return true;
    }

    /**
     * 以下两部分是针对 controller层第七部分："用户根据 id 查询，获取脱敏后的 UserVO（包装类）"
     * */

    /**
     * user 转换为 userVO
     * 获取脱敏后的单个用户信息
     * */
    @Override
    public UserVO getUserVO(User user) {
        if(user==null){
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user,userVO);
        return userVO;

    }

    /**
     *
     * 获取脱敏后的用户列表信息
     * */
    @Override
    public List<UserVO> getUserVOList(List<User> userlist) {
        if(CollUtil.isEmpty(userlist)) {
            return new ArrayList<>();
        }
        //return userlist.stream().map(user->this.getUserVO(user)).collect(Collectors.toList());
        return userlist.stream().map(this::getUserVO).collect(Collectors.toList());
    }


    /**
     * 将查询请求转化为 QueryWrapper对象 来生成 SQL 查询
     * */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if(userQueryRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空");
        }
        Long id=userQueryRequest.getId();
        String userAccount=userQueryRequest.getUserAccount();
        String userName=userQueryRequest.getUserName();
        String userRole=userQueryRequest.getUserRole();
        String userProfile=userQueryRequest.getUserProfile();
        String sortField=userQueryRequest.getSortField();
        String sortOrder=userQueryRequest.getSortOrder();


        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id),"id",id);
        queryWrapper.eq(ObjUtil.isNotNull(userRole),"user_role",userRole);
        queryWrapper.like(StrUtil.isNotBlank(userName),"user_name",userName);
        queryWrapper.like(StrUtil.isNotBlank(userAccount),"user_account",userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userProfile),"user_profile",userProfile);
        String sortColumn = StrUtil.isBlank(sortField)
                ? null
                : StrUtil.toUnderlineCase(sortField);

        queryWrapper.orderBy(sortColumn != null, "ascend".equals(sortOrder), sortColumn);
        //queryWrapper.orderBy(StrUtil.isNotEmpty(sortField),sortOrder.equals("asc"),"sort_field",sortField);

        return queryWrapper;
    }


    @Override
    public BaseResponse<User> getUserById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = this.getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(user);
    }

    @Override
    public boolean isadmin(User user) {
//        if(user==null){
//            return false;
//        }
//        if(!UserRoleEnums.ADMIN.getValue().equals(user.getUserRole())){
//            return false;
//        }
//        return true;

        return user!=null && UserRoleEnums.ADMIN.getValue().equals(user.getUserRole());

    }

    @Override
    public boolean deleteUser(long id) {
        // 1. 参数校验
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 2. 检查用户是否存在
        User user = this.getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        // 3. 【关键】禁止删除超级管理员 (假设 ID=1 是超管，根据你的实际项目调整)
        if (id == 1L) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无法删除超级管理员");
        }

        // 4. 【关键】禁止删除当前登录用户自己
        // 注意：Service 层通常不直接拿 request，这里可以通过 ThreadLocal 获取，
        // 或者简单点：让 Controller 传进来当前用户 ID，或者在 Controller 层先判断好再调 Service
        // 这里为了演示，假设我们暂时不校验“不能删自己”，或者你通过其他方式获取当前用户

        // 5. 执行删除 (MyBatis Plus 会根据 @TableLogic 自动变成 UPDATE ... set is_delete=1)
        boolean result = this.removeById(id);

        if (!result) {
            // 如果 MP 返回 false，说明 SQL 执行受影响行数为 0，可能是版本号冲突或逻辑删除配置问题
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除操作失败，请稍后重试");
        }

        return true;
    }


}




