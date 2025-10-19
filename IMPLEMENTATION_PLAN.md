# 登录安全方案实施计划

## 目标
实现基于 Redis + 滑动窗口的登录安全防护机制，防止暴力破解攻击。

## Stage 1: 环境准备与配置
**Goal**: 添加 Redis 依赖并配置连接
**Success Criteria**:
- Redis 依赖成功添加到 pom.xml
- application.yml 中 Redis 连接配置完成
- 应用能成功连接到 Redis

**Tasks**:
- [ ] 添加 Spring Data Redis 依赖
- [ ] 配置 Redis 连接信息
- [ ] 验证 Redis 连接

**Status**: ✅ Complete

---

## Stage 2: 安全配置类开发
**Goal**: 创建可配置的安全属性类
**Success Criteria**:
- LoginSecurityProperties 配置类创建完成
- 所有安全参数可通过 application.yml 配置
- 配置参数有合理的默认值

**Tasks**:
- [ ] 创建 LoginSecurityProperties 配置类
- [ ] 在 application.yml 中添加安全配置参数
- [ ] 添加配置验证

**Status**: ✅ Complete

---

## Stage 3: 核心安全服务实现
**Goal**: 实现 LoginSecurityService 核心逻辑
**Success Criteria**:
- 滑动窗口算法正确实现
- 账户锁定机制工作正常
- 失败记录清理逻辑完善
- 日志记录详细清晰

**Tasks**:
- [ ] 实现 recordLoginFail() - 记录登录失败
- [ ] 实现 isAccountLocked() - 检查账户锁定状态
- [ ] 实现 getRemainingLockTime() - 获取剩余锁定时间
- [ ] 实现 lockAccount() - 锁定账户
- [ ] 实现 clearLoginFail() - 清除失败记录
- [ ] 实现 unlockAccount() - 手动解锁账户
- [ ] 添加详细的中文注释说明 Redis 命令

**Status**: ✅ Complete

---

## Stage 4: 集成到登录流程
**Goal**: 将安全服务集成到现有登录接口
**Success Criteria**:
- 登录前检查账户锁定状态
- 登录失败时记录失败信息
- 登录成功时清除失败记录
- 能正确获取客户端 IP 地址

**Tasks**:
- [ ] 查找现有登录 Controller
- [ ] 添加安全检查逻辑
- [ ] 实现 IP 地址获取方法
- [ ] 完善错误提示信息

**Status**: ✅ Complete

---

## Stage 5: 测试与文档
**Goal**: 编写测试用例并完善文档
**Success Criteria**:
- 测试覆盖所有核心方法
- 测试用例能正常运行并通过
- LOGIN_SECURITY_SOLUTION.md 包含详细实施记录

**Tasks**:
- [ ] 编写 LoginSecurityServiceTest 测试类
- [ ] 测试滑动窗口逻辑
- [ ] 测试账户锁定与解锁
- [ ] 更新 LOGIN_SECURITY_SOLUTION.md 文档

**Status**: ✅ Complete

---

## 技术要点

### Redis 核心命令说明
1. **ZADD** - 向有序集合添加成员
2. **ZREMRANGEBYSCORE** - 删除分数区间内的成员
3. **ZCARD** - 获取有序集合成员数
4. **EXPIRE** - 设置键过期时间
5. **EXISTS** - 检查键是否存在
6. **TTL** - 获取键剩余生存时间

### 滑动窗口算法原理
- 使用 Redis Sorted Set（有序集合）存储失败时间戳
- 分数(score)为时间戳，值(value)也为时间戳
- 每次失败时添加新记录，删除窗口外的旧记录
- 统计集合大小即为窗口内失败次数

### 安全策略
1. 第1-2次失败：仅记录
2. 第3次失败（10秒内）：锁定账户15分钟
3. 锁定期间：拒绝所有登录尝试
4. 成功登录：清除所有失败记录
