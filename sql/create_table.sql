-- 创建库
create database if not exists lcoj;

-- 切换库
use lcoj;

-- 用户表
create table if not exists user
(
    id           bigint comment 'id' primary key,
    userName     varchar(256)                           not null comment '昵称',
    userPassword varchar(512)                           not null comment '密码',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin/ban',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    unique idx_userName (userName)
) comment '用户' collate = utf8mb4_unicode_ci;

-- 题目表
create table if not exists question
(
    id          bigint comment 'id' primary key,
    num         bigint                             not null comment '题号',
    title       varchar(512)                       not null comment '标题',
    content     text                               not null comment '内容',
    tags        varchar(1024)                      null comment '标签列表（json 数组）',
    answer      text                               null comment '题目标程',
    language    varchar(20)                        not null comment '标程语言',
    submitNum   int      default 0                 not null comment '题目提交数',
    acceptedNum int      default 0                 not null comment '题目通过数',
    sampleCase  text                               null comment '样例（json 数组）',
    judgeConfig text                               null comment '判题配置（json 对象）',
    userId      bigint                             not null comment '创建用户id',
    userName    varchar(256)                       not null comment '创建用户昵称',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除',
    index idx_userId (userId),
    index idx_num (num)
) comment '题目' collate = utf8mb4_unicode_ci;

-- 题目提交表
create table if not exists question_submit
(
    id            bigint comment 'id' primary key,
    language      varchar(128)                       not null comment '编程语言',
    code          text                               not null comment '用户代码',
    judgeResult   int      default 0                 not null comment '判题结果（0 - 等待判题、1 - 通过题目、 2~6 - 未通过、7 - 系统错误、 8 - 无测评数据）',
    maxTime       bigint                             null comment '最大耗时',
    maxMemory     bigint                             null comment '最大内存',
    caseInfoList  text                               null comment '每个点的判题信息（json 对象）',
    questionId    bigint                             not null comment '题目id',
    questionNum   bigint                             not null comment '题号',
    questionTitle varchar(512)                       not null comment '题目标题',
    userId        bigint                             not null comment '创建用户id',
    userName      varchar(256)                       not null comment '创建用户昵称',
    createTime    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete      tinyint  default 0                 not null comment '是否删除',
    index idx_questionId (questionId),
    index idx_userId (userId)
) comment '题目提交';

