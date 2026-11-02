package com.huahua.imagecloud.imagecloudserver.aop;


import com.huahua.imagecloud.imagecloudserver.annotation.AuthCheck;
import com.huahua.imagecloud.imagecloudserver.exception.BusinessException;
import com.huahua.imagecloud.imagecloudserver.exception.ErrorCode;
import com.huahua.imagecloud.imagecloudserver.model.entity.User;
import com.huahua.imagecloud.imagecloudserver.model.enums.UserRoleEnums;
import com.huahua.imagecloud.imagecloudserver.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AuthInterceptor {

    //获取当前登录用户信息。
    @Autowired
    private UserService userService;

    /**
     *  定义环绕通知（Around Advice）。
     * "@annotation(authCheck)" 是切入点表达式：
     * 只要发现某个方法上贴了 @AuthCheck 注解，就立刻拦截它，并把那个注解对象 mustRole 传给下面的 authCheck 参数。
     * */
    @Around("@annotation(authCheck)")

    /**
     * 执行拦截
     * @param joinPoint 切入点
     * @param authCheck 权限校验注解
     */
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {

        /**
         *  拦截 Controller 层中带有 @AuthCheck 注解的方法，执行一套标准的 “身份与权限提取流程”。
         */
/**
 *  先检查当前的登录用户
 * */

//        获取当前 HTTP 请求上下文：
//        RequestContextHolder 是 Spring 提供的工具，用于在非 Controller 层获取当前请求。
//        currentRequestAttributes() 返回当前请求的属性容器。
//        强制转换为 ServletRequestAttributes 后，调用 .getRequest() 得到原始的 HttpServletRequest 对象。
//        目的是为了能从中获取 Session  中的用户信息。
//         调用 UserService，根据 request 中的 Token/Cookie 解析出当前是谁在访问。
//         如果没登录，这个方法内部通常会直接抛异常。
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        User loginUser = userService.getLoginUser(request);
        // 获取当前用户具有的权限，方便下面比较
        UserRoleEnums userRoleEnum = UserRoleEnums.getEnumByValue(loginUser.getUserRole());


        // 从 @AuthCheck 注解中取出必须拥有的角色字符串（例如："ADMIN" 或 "USER"）
        String mustRole = authCheck.mustRole();
        //把注解里的字符串（如 "ADMIN"）转换成系统定义的 UserRoleEnum 枚举对象，方便比较。
        UserRoleEnums mustRoleEnum = UserRoleEnums.getEnumByValue(mustRole);

        /**
         * 判断是否需要权限校验。
         * */

        //注解值为null ，没有指定权限，直接放行
        if (mustRoleEnum == null) {
            return joinPoint.proceed();
        }

        /**
         *   以下为：必须有该权限才通过
         * */
        // 判断用户是否有任何角色，如果用户连个角色都没有（比如是未授权的游客），直接拒绝
        if (userRoleEnum == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"无权限");
        }
        // 要求必须有管理员权限，但当前用户没有管理员权限，拒绝
        //这里的逻辑处理是：“必须是管理员，但当前用户又不是管理员”
        if (UserRoleEnums.ADMIN.equals(mustRoleEnum) && !UserRoleEnums.ADMIN.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"无权限");
        }
        // 通过权限校验，放行
        return joinPoint.proceed();
    }
}
