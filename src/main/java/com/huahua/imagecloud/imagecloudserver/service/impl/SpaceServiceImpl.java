package com.huahua.imagecloud.imagecloudserver.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huahua.imagecloud.imagecloudserver.exception.BusinessException;
import com.huahua.imagecloud.imagecloudserver.exception.ErrorCode;
import com.huahua.imagecloud.imagecloudserver.exception.ThrowUtils;
import com.huahua.imagecloud.imagecloudserver.model.dto.space.SpaceAddRequest;
import com.huahua.imagecloud.imagecloudserver.model.dto.space.SpaceQueryRequest;
import com.huahua.imagecloud.imagecloudserver.model.entity.Picture;
import com.huahua.imagecloud.imagecloudserver.model.entity.Space;
import com.huahua.imagecloud.imagecloudserver.model.entity.User;
import com.huahua.imagecloud.imagecloudserver.model.enums.SpaceLevelEnum;
import com.huahua.imagecloud.imagecloudserver.model.vo.PictureVO;
import com.huahua.imagecloud.imagecloudserver.model.vo.SpaceVO;
import com.huahua.imagecloud.imagecloudserver.model.vo.UserVO;
import com.huahua.imagecloud.imagecloudserver.service.SpaceService;
import com.huahua.imagecloud.imagecloudserver.mapper.SpaceMapper;
import com.huahua.imagecloud.imagecloudserver.service.UserService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
* @author 28710
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2026-03-30 15:59:36
*/
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceService{

    @Autowired
    private UserService userService;

   @Autowired
   private TransactionTemplate transactionTemplate;

   @Autowired
   private RedissonClient redissonClient;
    /**
     * 开发效验空间数据的方法
     * */
    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space==null, ErrorCode.PARAMS_ERROR);

        // 从对象中取值
        String spaceName=space.getSpaceName();
        Integer spaceLevel=space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum=SpaceLevelEnum.getEnumByValue(spaceLevel);

        // 要创建空间
        if(add){
            if(StrUtil.isBlank(spaceName)){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间名称不能为空");
            }
            if(spaceLevel==null){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间级别不能为空");
            }
        }

        // 修改数据时，空间名称进行效验
        if(StrUtil.isNotBlank(spaceName) && spaceName.length()>32){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间名称过长");
        }

        // 对空间级别进行效验（万一用户随便抛了个数字）
        if(spaceLevel!=null && spaceLevelEnum==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间级别不存在");
        }
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        //根据空间级别，自动填充限额
        SpaceLevelEnum spaceLevelEnum=SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if(spaceLevelEnum !=null){
            //拿到枚举设置的最大空间额度
            long maxSize=spaceLevelEnum.getMaxSize();
            // 若是当前空间没有设置额度，则设置，
            if(space.getMaxSize()==null){
                space.setMaxSize(maxSize);
            }
            long maxCount=spaceLevelEnum.getMaxCount();
            if(space.getMaxCount()==null){
                space.setMaxCount(maxCount);
            }

        }
    }

    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        // 对象转封装类
        SpaceVO spaceVO=SpaceVO.objToVo(space);
        // 关联查询用户信息
        Long userId=space.getUserId();
        if(userId !=null && userId>=0){
            User user = userService.getById(userId);
            UserVO userVO=userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }
    

    /**
     * 分页获取图片封装
     *
     * 批量数据 脱敏返回
     */
    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        //从分页对象中提取出“当前页”的数据列表。
        List<Space> spaceList=spacePage.getRecords();
        Page<SpaceVO> spaceVOPage=new Page<>(spacePage.getCurrent(),spacePage.getSize(),spacePage.getTotal());
        if(CollUtil.isEmpty(spaceList)){
            return spaceVOPage;
        }

        // 把数据库查出来的space实体类 转换成 spaceVO类
        List<SpaceVO> spaceVOList=spaceList.stream().map(SpaceVO::objToVo).collect(Collectors.toList());

        /**
         * 处理 spaceVO中的user类
         * */
        // 根据数据库查出来的 space 实体类中的userId 去重
        Set<Long> userIdSet=spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());

        //拿着 userIdSet 查出所有关联的用户
        Map<Long,List<User>> userIdUserListMap=userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));

        // 拿着每个空间里的 userId，去那个 Map 里找到对应的用户信息，然后塞进 SpaceVO 的 user 字段里。
        spaceVOList.forEach(spaceVO->{
           Long userId=spaceVO.getUserId();

           User user=null;
           if(userIdUserListMap.containsKey(userId)){
               user=userIdUserListMap.get(userId).get(0);
           }

           spaceVO.setUser(userService.getUserVO(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;

    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();

        // 查询
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "user_id", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "space_name", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "space_level", spaceLevel);


        // 排序
        /**
         *
         *
         * @param StrUtil.isNotEmpty(sortField)
         * 如果 sortField 是空的（比如用户没传排序字段），这个表达式返回 false
         *
         * @param sortOrder.equals("ascend")
         * true 为 升序，false 为降序
         *
         * @param sortField
         * 生成SQL：orderby xxx（sortField ）=xxx
         *
         * */
        String sortColumn = StrUtil.isBlank(sortField)
                ? null
                : StrUtil.toUnderlineCase(sortField);

        queryWrapper.orderBy(sortColumn != null, "ascend".equals(sortOrder), sortColumn);
        return queryWrapper;
    }

    @Override
    public Long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        Space space=new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);

        /**
         * 1.填充参数默认值
         * */
        if(StrUtil.isBlank(spaceAddRequest.getSpaceName())){
            space.setSpaceName("默认空间");
        }
        if(spaceAddRequest.getSpaceLevel()==null){
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }

        this.fillSpaceBySpaceLevel(space);

        /**
         * 2.效验参数
         * */
        this.validSpace(space,true);

        /**
         * 3.效验权限，普通用户只能创建普通级别的空间
         * */
        Long userId=loginUser.getId();
        space.setUserId(userId);
        if(SpaceLevelEnum.COMMON.getValue()!=space.getSpaceLevel() && !userService.isadmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"无权限创建指定级别的空间");
        }

        /**
         * 4.控制同一用户只能创建一个私有空间 采用 加锁 + 事务 实现
         * */
//        String lock=String.valueOf(userId).intern();
//        synchronized (lock){
//            Long newSpaceId=transactionTemplate.execute(status -> {
//                // 先检查数据库是否已经存在userid
//                boolean exists=this.lambdaQuery().eq(Space::getUserId,userId).exists();
//                ThrowUtils.throwIf(exists,ErrorCode.OPERATION_ERROE,"每个用户仅能有一个私有空间");
//
//                //写入数据库
//                boolean result=this.save(space);
//                ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROE);
//
//                // 返回数据 id
//                return space.getId();
//            });
//            return Optional.ofNullable(newSpaceId).orElse(-1L);
//        }


            // =========== 3. Redisson 分布式锁 ===========
            RLock lock = redissonClient.getLock("SpaceAdd:" + userId);
            boolean tryLock;
            try {
                tryLock = lock.tryLock(3, 30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙");
            }
            ThrowUtils.throwIf(!tryLock, ErrorCode.OPERATION_ERROE, "请勿重复创建空间");

            try {
                Long newSpaceId = transactionTemplate.execute(status -> {
                    boolean exists = this.lambdaQuery().eq(Space::getUserId, userId).exists();
                    ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROE, "每个用户仅能有一个私有空间");
                    boolean result = this.save(space);
                    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROE);
                    return space.getId();
                });
                return Optional.ofNullable(newSpaceId).orElse(-1L);
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }



}




