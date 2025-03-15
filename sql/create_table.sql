-- 创建库
create database if not exists lcoj;

-- 切换库
use lcoj;

-- 用户表
CREATE TABLE `user`
(
    `id`           bigint                                  NOT NULL COMMENT 'id',
    `userName`     varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '昵称',
    `userPassword` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码',
    `userRole`     varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'user' COMMENT '用户角色：user/admin/ban',
    `createTime`   datetime                                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`   datetime                                NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`     tinyint                                 NOT NULL DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_userName` (`isDelete`, `userName`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户';

-- 题目表
CREATE TABLE `question`
(
    `id`          bigint                                  NOT NULL COMMENT 'id',
    `num`         bigint                                  NOT NULL COMMENT '题号',
    `title`       varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
    `content`     text COLLATE utf8mb4_unicode_ci         NOT NULL COMMENT '内容',
    `tags`        varchar(1024) COLLATE utf8mb4_unicode_ci         DEFAULT NULL COMMENT '标签列表（json 数组）',
    `answer`      text COLLATE utf8mb4_unicode_ci COMMENT '题目标程',
    `language`    varchar(20) COLLATE utf8mb4_unicode_ci  NOT NULL COMMENT '标程语言',
    `submitNum`   int                                     NOT NULL DEFAULT '0' COMMENT '题目提交数',
    `acceptedNum` int                                     NOT NULL DEFAULT '0' COMMENT '题目通过数',
    `sampleCase`  text COLLATE utf8mb4_unicode_ci COMMENT '样例（json 数组）',
    `judgeConfig` text COLLATE utf8mb4_unicode_ci COMMENT '判题配置（json 对象）',
    `userId`      bigint                                  NOT NULL COMMENT '创建用户id',
    `userName`    varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '创建用户昵称',
    `createTime`  datetime                                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`  datetime                                NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`    tinyint                                 NOT NULL DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_num` (`isDelete`, `num`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='题目';

-- 题目提交表
CREATE TABLE `question_submit`
(
    `id`            bigint       NOT NULL COMMENT 'id',
    `language`      varchar(128) NOT NULL COMMENT '编程语言',
    `code`          text         NOT NULL COMMENT '用户代码',
    `judgeResult`   int          NOT NULL DEFAULT '0' COMMENT '判题结果（0 - 等待判题、1 - 通过题目、 2~6 - 未通过、7 - 系统错误、 8 - 无测评数据）',
    `maxTime`       bigint                DEFAULT NULL COMMENT '最大耗时',
    `maxMemory`     bigint                DEFAULT NULL COMMENT '最大内存',
    `caseInfoList`  text COMMENT '每个点的判题信息（json 对象）',
    `questionId`    bigint       NOT NULL COMMENT '题目id',
    `questionNum`   bigint       NOT NULL COMMENT '题号',
    `questionTitle` varchar(512) NOT NULL COMMENT '题目标题',
    `userId`        bigint       NOT NULL COMMENT '创建用户id',
    `userName`      varchar(256) NOT NULL COMMENT '创建用户昵称',
    `createTime`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`      tinyint      NOT NULL DEFAULT '0' COMMENT '是否删除',
    KEY `idx_userName` (`userName`) USING BTREE,
    KEY `idx_language` (`language`, `createTime` DESC) USING BTREE,
    KEY `idx_judgeResult` (`judgeResult`, `createTime` DESC) USING BTREE,
    KEY `idx_num` (`questionNum`, `createTime` DESC) USING BTREE,
    KEY `idx_delete` (`isDelete`, `createTime` DESC) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='题目提交';

