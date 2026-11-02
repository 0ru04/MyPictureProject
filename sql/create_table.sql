-- 创建数据库
create database if not exists imagecloud;

-- 使用当前创建的数据库
use imagecloud;

-- 用户表
create table if not exists user
(
    id            bigint auto_increment                primary key    comment 'id' ,
    user_account  varchar(256)                         not null       comment '账号',
    user_password varchar(512)                         not null       comment'密码',
    user_name     varchar(256)                         null           comment'用户昵称',
    user_avatar   varchar(1024)                        null           comment '用户头像',
    user_profile  varchar(512)                         null           comment'用户简介',
    user_role     varchar(256)default'user'            not null       comment'用户角色：user/admin',
    edit_time     datetime default CURRENT_TIMESTAMP   not null       comment'编辑时间',
    create_time   datetime default CURRENT_TIMESTAMP   not null       comment'创建时间',
    update_time   datetime default CURRENT_TIMESTAMP   not null on update CURRENT_TIMESTAMP comment'更新时间',
    is_delete     tinyint  default 0                   not null       comment'是否删除',

    UNIQUE KEY uk_userAccout(user_account),
    INDEX idx_userName(user_name)
    )comment '用户' collate = utf8mb3_croatian_ci;


-- 图片表
create table if not exists picture
(
    id         bigint auto_increment comment 'id' primary key,
    url         varchar(512)                       not null comment '图片 url',
    name        varchar(128)                       not null comment '图片名称',
    introduction varchar(512)                       null comment '简介',
    category    varchar(64)                        null comment '分类',
    tags        varchar(512)                       null comment '标签 (JSON 数组)',
    pic_size     bigint                             null comment '图片体积',
    pic_width    int                                null comment '图片宽度',
    pic_height   int                                null comment '图片高度',
    pic_scale    double                             null comment '图片宽高比例',
    pic_format   varchar(32)                        null comment '图片格式',
    user_id      bigint                             not null comment '创建用户 id',
    create_time  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    edit_time    datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    update_time  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete    tinyint  default 0                 not null comment '是否删除',
    INDEX idx_name (name),              -- 提升基于图片名称的查询性能
    INDEX idx_introduction (introduction), -- 用于模糊搜索图片简介
    INDEX idx_category (category),        -- 提升基于分类的查询性能
    INDEX idx_tags (tags),                -- 提升基于标签的查询性能
    INDEX idx_userId (user_id)             -- 提升基于用户 ID 的查询性能
    ) comment '图片' collate = utf8mb4_unicode_ci;


alter table picture
 -- 添加新列
 add column review_status int default 0 not null comment '审核状态：0-待审核；1-通过，2-拒绝',
 add column review_message varchar(512) null comment '审核信息',
 add column reviewer_id bigint null comment '审核人ID',
 add column review_time datetime null  comment '审核时间';

-- 创建基于reviewStatus 列的索引
create index idx_reviewStatus on picture(review_status);

alter table picture add column thumbnail_url varchar(512) null comment '缩略图url';

ALTER TABLE picture MODIFY COLUMN name VARCHAR(255) DEFAULT '未命名图片' NOT NULL;



-- 空间表
create table if not exists space
(
    id           bigint auto_increment comment 'id' primary key,
    space_name    varchar(128)                       null comment '空间名称',
    space_level   int              default 0         null comment '空间级别：0-普通版 1-专业版 2-旗舰版',
    max_size      bigint           default 0         null comment '空间图片的最大总大小',
    max_count     bigint           default 0         null comment '空间图片的最大数量',
    total_size    bigint           default 0         null comment '当前空间下图片的总大小',
    total_count   bigint           default 0         null comment '当前空间下的图片数量',
    user_id       bigint                           not null comment '创建用户 id',
    create_time   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    edit_time     datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete     tinyint          default 0         not null comment '是否删除',
    -- 索引设计
    index idx_userId (user_id), -- 提升基于用户的查询效率
    index idx_spaceName (space_name), -- 提升基于空间名称的查询效率
    index idx_spaceLevel (space_level) -- 提升按空间级别查询的效率
) comment '空间' collate = utf8mb4_unicode_ci;

-- 添加新列
ALTER TABLE picture
    ADD COLUMN space_id bigint null comment '空间 id（为空表示公共空间）';

-- 创建索引
CREATE INDEX idx_spaceId ON picture (space_id);


alter  table picture add column pic_color varchar(16) null comment '图片主色调';


ALTER TABLE picture ADD COLUMN origin_url VARCHAR(512) DEFAULT NULL COMMENT '原图URL';



















