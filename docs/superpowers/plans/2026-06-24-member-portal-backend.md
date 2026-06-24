# 会员门户 (member-portal.html) 后端对接 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 `frontend/member-portal.html` 打通会员"手机号 + 姓名"登录、数据回填、分页消费记录三大能力。

**Architecture:**
- 后端新增 3 个端点（`/member/login/match`、`/member/login`、`/member/{id}/transactions/page`），复用现有 JWT 但通过 `subject` 前缀区分会员/管理员。
- `AuthUser` 增加 `type` 字段；`JwtAuthenticationFilter` 增加 `member:` 前缀解析分支。
- 前端 `member-portal.html` 全量重写：登录蒙层 + 主页面数据渲染 + 交易记录 Modal（含加载更多）。

**Tech Stack:** Spring Boot 2.7.18、MyBatis-Plus 3.5.9、JWT (jjwt 0.12.5)、原生 HTML/JS（无构建步骤）。

---

## 现有代码约定（实施前必读）

- Controller 全部用 `ApiResponse<T>` 包装返回（`src/main/java/com/slm/barbershop/model/ApiResponse.java`）。
- 业务异常用 `BizException` 抛出，全局异常处理统一返回。
- Service 命名遵循"实体 + Service"。
- DTO/VO 与 Entity 互转用 `converter/` 下的 MapStruct（已存在 `MemberConverter`、`MemberTransactionConverter`）。
- 已有 `PageHandlerMethodArgumentResolver` 支持 `IPage` 类型参数解析；本次复用。
- 已有 `GlobalCorsConfig` 允许所有 origin（`file://` 打开也能跨域请求）。

---

## Task 1: AuthUser 增加 `type` 字段

**Files:**
- Modify: `src/main/java/com/slm/barbershop/model/AuthUser.java`

- [ ] **Step 1: 修改 AuthUser**

完整替换文件内容：

```java
package com.slm.barbershop.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证用户信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthUser {

    public static final String TYPE_USER = "user";
    public static final String TYPE_MEMBER = "member";

    private Long id;
    private String username;
    private String type;

    public AuthUser(Long id, String username) {
        this(id, username, TYPE_USER);
    }

    public boolean isMember() {
        return TYPE_MEMBER.equals(type);
    }

}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/slm/barbershop/model/AuthUser.java
git commit -m "feat(auth): AuthUser 增加 type 字段区分用户/会员"
```

---

## Task 2: JWTUtil 支持自定义 subject

**Files:**
- Modify: `src/main/java/com/slm/barbershop/utils/JWTUtil.java`

- [ ] **Step 1: 新增带 subject 的重载方法**

保留原有 `generateJwtToken(Long, String)` 不动（控制台用户仍用），新增：

```java
public String generateJwtToken(Long id, String username, String subject) {
    return Jwts.builder()
            .header()
            .add("typ", "JWT")
            .add("alg", "HS256")
            .and()
            .claim("id", id)
            .claim("username", username)
            .id(UUID.randomUUID().toString())
            .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration * 1000))
            .issuedAt(new Date())
            .subject(subject)
            .issuer(issuer)
            .signWith(key, ALGORITHM)
            .compact();
}
```

完整文件最终形态：

```java
package com.slm.barbershop.utils;

import com.slm.barbershop.exception.BizException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecureDigestAlgorithm;
import io.jsonwebtoken.security.SecurityException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
public class JWTUtil {

    private final static SecureDigestAlgorithm<SecretKey, SecretKey> ALGORITHM = Jwts.SIG.HS256;

    public static final String MEMBER_TOKEN_SUBJECT_PREFIX = "member:";

    private final SecretKey key;
    private final long accessTokenExpiration;
    private final String issuer = "barbershop";
    private final String subject = "barbershop-user";

    public JWTUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public String generateJwtToken(Long id, String username) {
        return generateJwtToken(id, username, subject);
    }

    public String generateJwtToken(Long id, String username, String subject) {
        return Jwts.builder()
                .header()
                .add("typ", "JWT")
                .add("alg", "HS256")
                .and()
                .claim("id", id)
                .claim("username", username)
                .id(UUID.randomUUID().toString())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration * 1000))
                .issuedAt(new Date())
                .subject(subject)
                .issuer(issuer)
                .signWith(key, ALGORITHM)
                .compact();
    }

    public Claims getClaimsFromJwt(String jwt) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(jwt).getPayload();
        } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException | SecurityException | IllegalArgumentException e) {
            throw new BizException(HttpStatus.BAD_REQUEST, "无效token");
        }
    }

    public Long getUserIdFromJwt(String jwt) {
        Claims claims = getClaimsFromJwt(jwt);
        return claims.get("id", Long.class);
    }

    public String getUsernameFromJwt(String jwt) {
        Claims claims = getClaimsFromJwt(jwt);
        return claims.get("username", String.class);
    }

}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/slm/barbershop/utils/JWTUtil.java
git commit -m "feat(auth): JWTUtil 支持自定义 subject,用于签发会员 token"
```

---

## Task 3: JwtAuthenticationFilter 解析 member token

**Files:**
- Modify: `src/main/java/com/slm/barbershop/config/JwtAuthenticationFilter.java`

- [ ] **Step 1: 修改 EXCLUDE_PATHS 与解析逻辑**

完整替换文件内容：

```java
package com.slm.barbershop.config;

import com.slm.barbershop.model.AuthUser;
import com.slm.barbershop.utils.JWTUtil;
import com.slm.barbershop.utils.ResponseUtil;
import com.slm.barbershop.utils.UserContext;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter implements Filter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String AUTH_HEADER_TYPE = "Bearer";

    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
            "/auth/login",
            "/auth/register",
            "/auth/send-email-code",
            "/member/login",
            "/member/login/match",
            "/swagger-ui",
            "/v3/api-docs",
            "/swagger-resources",
            "/webjars",
            "/favicon.ico"
    );

    @Autowired
    private JWTUtil jwtUtil;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        String pathWithoutContext = path.substring(contextPath.length());

        if (isExcludedPath(pathWithoutContext)) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = httpRequest.getHeader(AUTH_HEADER);
        if (StringUtils.isEmpty(authHeader) || !authHeader.startsWith(AUTH_HEADER_TYPE)) {
            ResponseUtil.setResponse(httpResponse, HttpStatus.UNAUTHORIZED, "未登录");
            return;
        }

        String authToken = authHeader.split(" ")[1];
        log.debug("authToken: {}", authToken);

        try {
            Claims claims = jwtUtil.getClaimsFromJwt(authToken);
            Long userId = claims.get("id", Long.class);
            String username = claims.get("username", String.class);
            String subject = claims.getSubject();

            String type = AuthUser.TYPE_USER;
            if (subject != null && subject.startsWith(JWTUtil.MEMBER_TOKEN_SUBJECT_PREFIX)) {
                type = AuthUser.TYPE_MEMBER;
            }

            AuthUser authUser = new AuthUser(userId, username, type);
            UserContext.setUser(authUser);

            chain.doFilter(request, response);
        } catch (Exception e) {
            log.error("JWT authentication failed", e);
            ResponseUtil.setResponse(httpResponse, HttpStatus.UNAUTHORIZED, "无效token");
        } finally {
            UserContext.clear();
        }
    }

    private boolean isExcludedPath(String path) {
        return EXCLUDE_PATHS.stream().anyMatch(path::startsWith);
    }

}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/slm/barbershop/config/JwtAuthenticationFilter.java
git commit -m "feat(auth): JwtAuthenticationFilter 识别 member token,排除 /member/login 路径"
```

---

## Task 4: 新增 MemberMatchVO / MemberLoginRequest DTO

**Files:**
- Create: `src/main/java/com/slm/barbershop/model/MemberMatchVO.java`
- Create: `src/main/java/com/slm/barbershop/model/MemberLoginRequest.java`

- [ ] **Step 1: 创建 MemberMatchVO**

```java
package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会员跨店铺匹配结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "会员跨店铺匹配结果")
public class MemberMatchVO {

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "所属店铺ID")
    private Long shopId;

    @Schema(description = "店铺名称")
    private String shopName;

    @Schema(description = "会员姓名")
    private String name;

    @Schema(description = "手机号")
    private String phone;

}
```

- [ ] **Step 2: 创建 MemberLoginRequest**

```java
package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 会员登录请求
 */
@Data
@Schema(description = "会员登录请求")
public class MemberLoginRequest {

    @Schema(description = "手机号", required = true, example = "13800008888")
    private String phone;

    @Schema(description = "姓名", required = true, example = "李明")
    private String name;

    @Schema(description = "店铺ID", required = true, example = "1")
    private Long shopId;

}
```

- [ ] **Step 3: 创建 MemberTransactionPageVO**

```java
package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 会员交易分页响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "会员交易分页响应")
public class MemberTransactionPageVO {

    @Schema(description = "当前页数据")
    private List<MemberTransactionResponse> records;

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "当前页(从1开始)")
    private Long page;

    @Schema(description = "每页大小")
    private Long size;

    @Schema(description = "是否还有更多")
    private Boolean hasMore;

}
```

- [ ] **Step 4: 编译验证**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/slm/barbershop/model/MemberMatchVO.java \
        src/main/java/com/slm/barbershop/model/MemberLoginRequest.java \
        src/main/java/com/slm/barbershop/model/MemberTransactionPageVO.java
git commit -m "feat(member): 新增会员登录/匹配/分页响应 DTO"
```

---

## Task 5: MemberMapper 新增 matchAcrossShops 查询

**Files:**
- Modify: `src/main/java/com/slm/barbershop/mapper/MemberMapper.java`
- Create: `src/main/resources/mapper/MemberMapper.xml`

- [ ] **Step 1: 修改 MemberMapper**

```java
package com.slm.barbershop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.slm.barbershop.entity.Member;
import com.slm.barbershop.model.MemberMatchVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MemberMapper extends BaseMapper<Member> {

    List<MemberMatchVO> matchAcrossShops(@Param("phone") String phone, @Param("name") String name);

}
```

- [ ] **Step 2: 创建 MemberMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.slm.barbershop.mapper.MemberMapper">

    <resultMap id="MemberMatchVO" type="com.slm.barbershop.model.MemberMatchVO">
        <id property="memberId" column="member_id" />
        <result property="shopId" column="shop_id" />
        <result property="shopName" column="shop_name" />
        <result property="name" column="name" />
        <result property="phone" column="phone" />
    </resultMap>

    <select id="matchAcrossShops" resultMap="MemberMatchVO">
        SELECT m.id   AS member_id,
               m.shop_id,
               s.name AS shop_name,
               m.name,
               m.phone
        FROM member m
        INNER JOIN shop s ON s.id = m.shop_id AND s.is_deleted = 0
        WHERE m.phone = #{phone}
          AND m.name  = #{name}
          AND m.is_deleted = 0
        ORDER BY m.id ASC
    </select>

</mapper>
```

- [ ] **Step 3: 编译验证**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/slm/barbershop/mapper/MemberMapper.java \
        src/main/resources/mapper/MemberMapper.xml
git commit -m "feat(member): 新增跨店铺会员匹配查询"
```

---

## Task 6: MemberService 新增 match / login 入口

**Files:**
- Modify: `src/main/java/com/slm/barbershop/service/MemberService.java`

- [ ] **Step 1: 在 MemberService 中新增 match 方法**

完整替换文件：

```java
package com.slm.barbershop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slm.barbershop.converter.MemberConverter;
import com.slm.barbershop.entity.Member;
import com.slm.barbershop.exception.BizException;
import com.slm.barbershop.mapper.MemberMapper;
import com.slm.barbershop.model.MemberMatchVO;
import com.slm.barbershop.model.MemberRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Service
public class MemberService extends ServiceImpl<MemberMapper, Member> {

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private MemberConverter memberConverter;

    public Member create(Long shopId, MemberRequest request) {
        Member member = memberConverter.toEntityWithShopId(request, shopId);
        member.setBalance(BigDecimal.ZERO);
        memberMapper.insert(member);
        return member;
    }

    public Member update(Long shopId, Long id, MemberRequest request) {
        Member member = getByIdAndShopId(shopId, id);
        if (member == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "会员不存在");
        }
        // 只更新 name / phone，余额由 MemberTransactionService 单独维护
        member.setName(request.getName());
        member.setPhone(request.getPhone());
        memberMapper.updateById(member);
        return member;
    }

    public void delete(Long shopId, Long id) {
        Member member = getByIdAndShopId(shopId, id);
        if (member == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "会员不存在");
        }
        memberMapper.deleteById(id);
    }

    public Member getById(Long shopId, Long id) {
        return getByIdAndShopId(shopId, id);
    }

    public List<Member> listByShopId(Long shopId) {
        return memberMapper.selectList(
                new LambdaQueryWrapper<Member>()
                        .eq(Member::getShopId, shopId)
        );
    }

    /**
     * 跨店铺查找匹配手机号+姓名的会员。
     * 入参校验由 Controller 完成；这里只负责查询。
     */
    public List<MemberMatchVO> match(String phone, String name) {
        if (phone == null || phone.isEmpty() || name == null || name.isEmpty()) {
            return Collections.emptyList();
        }
        return memberMapper.matchAcrossShops(phone.trim(), name.trim());
    }

    private Member getByIdAndShopId(Long shopId, Long id) {
        return memberMapper.selectOne(
                new LambdaQueryWrapper<Member>()
                        .eq(Member::getShopId, shopId)
                        .eq(Member::getId, id)
        );
    }

}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/slm/barbershop/service/MemberService.java
git commit -m "feat(member): MemberService 新增 match 跨店铺查询"
```

---

## Task 7: 新增 MemberAuthService

**Files:**
- Create: `src/main/java/com/slm/barbershop/service/MemberAuthService.java`

- [ ] **Step 1: 创建文件**

```java
package com.slm.barbershop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.slm.barbershop.entity.Member;
import com.slm.barbershop.exception.BizException;
import com.slm.barbershop.mapper.MemberMapper;
import com.slm.barbershop.model.LoginResponse;
import com.slm.barbershop.utils.JWTUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class MemberAuthService {

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private JWTUtil jwtUtil;

    /**
     * 会员登录：phone + name + shopId 三项校验,签发 JWT
     */
    public LoginResponse login(String phone, String name, Long shopId) {
        if (phone == null || phone.isEmpty()
                || name == null || name.isEmpty()
                || shopId == null) {
            throw new BizException(HttpStatus.BAD_REQUEST, "参数不完整");
        }

        Member member = memberMapper.selectOne(
                new LambdaQueryWrapper<Member>()
                        .eq(Member::getShopId, shopId)
                        .eq(Member::getPhone, phone.trim())
                        .eq(Member::getName, name.trim())
                        .eq(Member::getIsDeleted, 0)
                        .last("LIMIT 1")
        );

        if (member == null) {
            throw new BizException(HttpStatus.UNAUTHORIZED, "姓名或手机号不正确");
        }

        String subject = JWTUtil.MEMBER_TOKEN_SUBJECT_PREFIX + member.getId();
        String token = jwtUtil.generateJwtToken(member.getId(), member.getName(), subject);
        return new LoginResponse(token, member.getId(), member.getName());
    }

}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/slm/barbershop/service/MemberAuthService.java
git commit -m "feat(member): 新增 MemberAuthService 处理会员登录"
```

---

## Task 8: MemberTransactionService 新增分页查询

**Files:**
- Modify: `src/main/java/com/slm/barbershop/service/MemberTransactionService.java`

- [ ] **Step 1: 新增 pageByMemberId 方法**

在现有 `listByMemberId` 方法之后追加：

```java
public com.slm.barbershop.model.MemberTransactionPageVO pageByMemberId(Long memberId, long page, long size) {
    long p = Math.max(page, 1);
    long s = Math.min(Math.max(size, 1), 100);
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<MemberTransaction> mpPage =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(p, s);
    com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MemberTransaction> wrapper =
            new LambdaQueryWrapper<MemberTransaction>()
                    .eq(MemberTransaction::getMemberId, memberId)
                    .orderByDesc(MemberTransaction::getCreatedTime)
                    .orderByDesc(MemberTransaction::getId);
    com.baomidou.mybatisplus.core.metadata.IPage<MemberTransaction> result =
            transactionMapper.selectPage(mpPage, wrapper);
    List<MemberTransaction> records = result.getRecords();
    long total = result.getTotal();
    boolean hasMore = p * s < total;
    return new com.slm.barbershop.model.MemberTransactionPageVO(
            transactionConverter.toResponseList(records),
            total, p, s, hasMore);
}
```

并在类顶部添加 import 与 `transactionConverter` 字段。完整类最终形态：

```java
package com.slm.barbershop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slm.barbershop.converter.MemberTransactionConverter;
import com.slm.barbershop.entity.Member;
import com.slm.barbershop.entity.MemberTransaction;
import com.slm.barbershop.enums.TransactionType;
import com.slm.barbershop.exception.BizException;
import com.slm.barbershop.lock.DistributedLock;
import com.slm.barbershop.mapper.MemberMapper;
import com.slm.barbershop.mapper.MemberTransactionMapper;
import com.slm.barbershop.model.MemberTransactionPageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MemberTransactionService extends ServiceImpl<MemberTransactionMapper, MemberTransaction> {

    @Autowired
    private MemberTransactionMapper transactionMapper;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private MemberTransactionConverter transactionConverter;

    /**
     * 注入自身代理,使 {@link #doUpdate} 上的 @Transactional 通过代理生效
     */
    @Autowired
    @Lazy
    private MemberTransactionService self;

    public MemberTransaction store(Long memberId, BigDecimal amount, String remark, String idempotencyKey) {
        return applyBalance(memberId, amount, TransactionType.STORE, remark, idempotencyKey);
    }

    public MemberTransaction consume(Long memberId, BigDecimal amount, String remark, String idempotencyKey) {
        return applyBalance(memberId, amount, TransactionType.CONSUME, remark, idempotencyKey);
    }

    /**
     * 公共余额变更流程:
     * 1. 幂等前置查询(命中 → 直接返回原流水)
     * 2. 走锁策略,在事务内重试:读 member → 计算余额 → updateById(CAS) → 写流水
     */
    private MemberTransaction applyBalance(Long memberId,
                                           BigDecimal amount,
                                           TransactionType type,
                                           String remark,
                                           String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            MemberTransaction existed = findByIdempotencyKey(idempotencyKey);
            if (existed != null) {
                return existed;
            }
        }

        return self.doUpdate(memberId, amount, type, remark, idempotencyKey);
    }

    /**
     * 单次带事务的余额变更;乐观锁冲突时抛 {@link OptimisticLockingFailureException} 由策略重试
     */
    @DistributedLock(key = "'member_transaction:' + #memberId", mode = DistributedLock.Mode.REDIS)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public MemberTransaction doUpdate(Long memberId,
                                      BigDecimal amount,
                                      TransactionType type,
                                      String remark,
                                      String idempotencyKey) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "会员不存在");
        }

        BigDecimal balanceBefore = member.getBalance();
        BigDecimal balanceAfter;
        if (type == TransactionType.CONSUME) {
            if (balanceBefore.compareTo(amount) < 0) {
                throw new BizException(HttpStatus.BAD_REQUEST, "余额不足");
            }
            balanceAfter = balanceBefore.subtract(amount);
        } else {
            balanceAfter = balanceBefore.add(amount);
        }

        member.setBalance(balanceAfter);
        int rows = memberMapper.updateById(member);
        if (rows == 0) {
            // @Version 触发 CAS 失败,抛出让策略重试
            throw new OptimisticLockingFailureException(
                    "member balance update conflict, memberId=" + memberId);
        }

        MemberTransaction transaction = new MemberTransaction();
        transaction.setMemberId(memberId);
        transaction.setType(type.name());
        transaction.setAmount(amount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setRemark(remark);
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setCreatedTime(LocalDateTime.now());
        try {
            transactionMapper.insert(transaction);
        } catch (DuplicateKeyException e) {
            // 并发同 idempotencyKey,事务回滚,余额一并撤销
            throw new BizException(HttpStatus.CONFLICT, "重复提交");
        }

        return transaction;
    }

    private MemberTransaction findByIdempotencyKey(String idempotencyKey) {
        return transactionMapper.selectOne(
                new LambdaQueryWrapper<MemberTransaction>()
                        .eq(MemberTransaction::getIdempotencyKey, idempotencyKey)
                        .last("LIMIT 1")
        );
    }

    public BigDecimal getBalance(Long memberId) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "会员不存在");
        }
        return member.getBalance();
    }

    public List<MemberTransaction> listByMemberId(Long memberId) {
        return transactionMapper.selectList(
                new LambdaQueryWrapper<MemberTransaction>()
                        .eq(MemberTransaction::getMemberId, memberId)
                        .orderByDesc(MemberTransaction::getCreatedTime)
        );
    }

    /**
     * 分页查询交易流水,按 created_time DESC, id DESC 排序。
     * page 从 1 开始,size 默认 20,上限 100。
     */
    public MemberTransactionPageVO pageByMemberId(Long memberId, long page, long size) {
        long p = Math.max(page, 1);
        long s = Math.min(Math.max(size, 1), 100);
        Page<MemberTransaction> mpPage = new Page<>(p, s);
        LambdaQueryWrapper<MemberTransaction> wrapper = new LambdaQueryWrapper<MemberTransaction>()
                .eq(MemberTransaction::getMemberId, memberId)
                .orderByDesc(MemberTransaction::getCreatedTime)
                .orderByDesc(MemberTransaction::getId);
        IPage<MemberTransaction> result = transactionMapper.selectPage(mpPage, wrapper);
        List<MemberTransaction> records = result.getRecords();
        long total = result.getTotal();
        boolean hasMore = p * s < total;
        return new MemberTransactionPageVO(
                transactionConverter.toResponseList(records),
                total, p, s, hasMore);
    }

}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/slm/barbershop/service/MemberTransactionService.java
git commit -m "feat(transaction): 新增交易流水分页查询 pageByMemberId"
```

---

## Task 9: MemberController 新增 3 个端点 + 鉴权校验

**Files:**
- Modify: `src/main/java/com/slm/barbershop/controller/MemberController.java`

- [ ] **Step 1: 注入 MemberAuthService 并新增端点**

完整替换文件：

```java
package com.slm.barbershop.controller;

import com.slm.barbershop.converter.MemberConverter;
import com.slm.barbershop.converter.MemberTransactionConverter;
import com.slm.barbershop.entity.Member;
import com.slm.barbershop.entity.MemberTransaction;
import com.slm.barbershop.exception.BizException;
import com.slm.barbershop.model.ApiResponse;
import com.slm.barbershop.model.LoginResponse;
import com.slm.barbershop.model.MemberLoginRequest;
import com.slm.barbershop.model.MemberMatchVO;
import com.slm.barbershop.model.MemberRequest;
import com.slm.barbershop.model.MemberResponse;
import com.slm.barbershop.model.MemberTransactionPageVO;
import com.slm.barbershop.model.MemberTransactionRequest;
import com.slm.barbershop.model.MemberTransactionResponse;
import com.slm.barbershop.service.MemberAuthService;
import com.slm.barbershop.service.MemberService;
import com.slm.barbershop.service.MemberTransactionService;
import com.slm.barbershop.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "会员", description = "会员管理相关接口")
@RestController
@RequestMapping("/member")
public class MemberController {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberConverter memberConverter;

    @Autowired
    private MemberTransactionService transactionService;

    @Autowired
    private MemberTransactionConverter transactionConverter;

    @Autowired
    private MemberAuthService memberAuthService;

    @Operation(summary = "创建会员")
    @PostMapping
    public ApiResponse<MemberResponse> create(@RequestBody MemberRequest request) {
        Member member = memberService.create(request.getShopId(), request);
        return ApiResponse.ok(memberConverter.toResponse(member));
    }

    @Operation(summary = "更新会员")
    @Parameter(name = "id", description = "会员ID", in = ParameterIn.PATH)
    @PutMapping("/{id}")
    public ApiResponse<MemberResponse> update(@PathVariable Long id, @RequestBody MemberRequest request) {
        Member member = memberService.update(request.getShopId(), id, request);
        return ApiResponse.ok(memberConverter.toResponse(member));
    }

    @Operation(summary = "删除会员")
    @Parameter(name = "id", description = "会员ID", in = ParameterIn.PATH)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @RequestParam Long shopId) {
        memberService.delete(shopId, id);
        return ApiResponse.ok();
    }

    @Operation(summary = "获取会员")
    @Parameter(name = "id", description = "会员ID", in = ParameterIn.PATH)
    @GetMapping("/{id}")
    public ApiResponse<MemberResponse> getById(@PathVariable Long id, @RequestParam Long shopId) {
        Member member = memberService.getById(shopId, id);
        return ApiResponse.ok(memberConverter.toResponse(member));
    }

    @Operation(summary = "获取店铺会员列表")
    @GetMapping("/list")
    public ApiResponse<List<MemberResponse>> list(@RequestParam Long shopId) {
        List<Member> members = memberService.listByShopId(shopId);
        return ApiResponse.ok(memberConverter.toResponseList(members));
    }

    @Operation(summary = "储值")
    @Parameter(name = "id", description = "会员ID", in = ParameterIn.PATH)
    @PostMapping("/{id}/store")
    public ApiResponse<MemberTransactionResponse> store(@PathVariable Long id, @RequestBody MemberTransactionRequest request) {
        MemberTransaction transaction = transactionService.store(id, request.getAmount(), request.getRemark(), request.getIdempotencyKey());
        return ApiResponse.ok(transactionConverter.toResponse(transaction));
    }

    @Operation(summary = "消费")
    @Parameter(name = "id", description = "会员ID", in = ParameterIn.PATH)
    @PostMapping("/{id}/consume")
    public ApiResponse<MemberTransactionResponse> consume(@PathVariable Long id, @RequestBody MemberTransactionRequest request) {
        MemberTransaction transaction = transactionService.consume(id, request.getAmount(), request.getRemark(), request.getIdempotencyKey());
        return ApiResponse.ok(transactionConverter.toResponse(transaction));
    }

    @Operation(summary = "查询余额")
    @Parameter(name = "id", description = "会员ID", in = ParameterIn.PATH)
    @GetMapping("/{id}/balance")
    public ApiResponse<BigDecimal> getBalance(@PathVariable Long id) {
        assertMemberSelf(id);
        BigDecimal balance = transactionService.getBalance(id);
        return ApiResponse.ok(balance);
    }

    @Operation(summary = "交易流水")
    @Parameter(name = "id", description = "会员ID", in = ParameterIn.PATH)
    @GetMapping("/{id}/transactions")
    public ApiResponse<List<MemberTransactionResponse>> transactions(@PathVariable Long id) {
        assertMemberSelf(id);
        List<MemberTransaction> transactions = transactionService.listByMemberId(id);
        return ApiResponse.ok(transactionConverter.toResponseList(transactions));
    }

    // ====== 新增：会员门户相关接口 ======

    @Operation(summary = "跨店铺查找会员(按手机号+姓名)")
    @GetMapping("/login/match")
    public ApiResponse<List<MemberMatchVO>> matchForLogin(
            @RequestParam String phone,
            @RequestParam String name) {
        if (phone == null || phone.trim().isEmpty() || name == null || name.trim().isEmpty()) {
            throw new BizException(HttpStatus.BAD_REQUEST, "手机号和姓名不能为空");
        }
        return ApiResponse.ok(memberService.match(phone, name));
    }

    @Operation(summary = "会员登录(返回JWT)")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> loginForMember(@RequestBody MemberLoginRequest request) {
        LoginResponse response = memberAuthService.login(
                request.getPhone(), request.getName(), request.getShopId());
        return ApiResponse.ok(response);
    }

    @Operation(summary = "会员交易流水分页查询")
    @Parameter(name = "id", description = "会员ID", in = ParameterIn.PATH)
    @GetMapping("/{id}/transactions/page")
    public ApiResponse<MemberTransactionPageVO> transactionsPage(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        assertMemberSelf(id);
        return ApiResponse.ok(transactionService.pageByMemberId(id, page, size));
    }

    /**
     * 校验当前请求是否来自会员本人。
     * 仅会员身份的 token 且 memberId 与 path 中 {id} 一致时通过。
     * 后台用户（管理员）调用同样放行,便于管理员代查。
     */
    private void assertMemberSelf(Long pathMemberId) {
        var user = UserContext.getUser();
        if (user == null) {
            throw new BizException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        if (user.isMember() && !user.getId().equals(pathMemberId)) {
            throw new BizException(HttpStatus.FORBIDDEN, "无权访问该会员数据");
        }
    }

}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 启动应用并冒烟测试**

Run: `mvn spring-boot:run`（后台启动）

等待启动后另开终端：

```bash
# 1. 匹配查询
curl "http://localhost:8080/barbershop/member/login/match?phone=13800008888&name=李明"
# 期望: code=0, data 为数组(可能为空)

# 2. 登录接口（需要已有数据）
curl -X POST "http://localhost:8080/barbershop/member/login" \
     -H "Content-Type: application/json" \
     -d '{"phone":"13800008888","name":"李明","shopId":1}'
# 期望: code=0, data.token 存在
```

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/slm/barbershop/controller/MemberController.java
git commit -m "feat(member): 新增 /member/login/match、/login、/{id}/transactions/page"
```

---

## Task 10: 前端重写 member-portal.html（登录蒙层 + 数据回填 + 交易 Modal）

**Files:**
- Modify: `frontend/member-portal.html`（全量重写）

- [ ] **Step 1: 完整重写文件**

完整替换文件内容：

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>会员中心</title>
  <style>
      * { margin: 0; padding: 0; box-sizing: border-box; }
      body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f5f5f7; min-height: 100vh; }
      .container { max-width: 375px; margin: 0 auto; padding: 20px 16px; }

      /* Profile Card */
      .profile-card { background: #fff; border-radius: 16px; padding: 24px; margin-bottom: 16px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
      .profile-header { display: flex; align-items: center; gap: 16px; margin-bottom: 20px; }
      .avatar { width: 64px; height: 64px; border-radius: 50%; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); display: flex; align-items: center; justify-content: center; color: #fff; font-size: 24px; font-weight: 600; }
      .profile-info h1 { font-size: 20px; color: #1a1a1a; margin-bottom: 4px; }
      .profile-info p { font-size: 13px; color: #8e8e93; }
      .store-badge { display: inline-flex; align-items: center; gap: 6px; background: #f0f0f5; padding: 6px 12px; border-radius: 20px; font-size: 12px; color: #666; margin-top: 12px; }
      .store-badge::before { content: '📍'; }

      /* Balance Card */
      .balance-card { background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%); border-radius: 16px; padding: 28px 24px; margin-bottom: 16px; color: #fff; }
      .balance-label { font-size: 13px; opacity: 0.7; margin-bottom: 8px; letter-spacing: 1px; }
      .balance-amount { font-size: 42px; font-weight: 700; letter-spacing: -1px; }
      .balance-amount::before { content: '¥'; font-size: 24px; margin-right: 4px; }
      .balance-hint { font-size: 12px; opacity: 0.5; margin-top: 8px; }

      /* Menu */
      .menu-section { background: #fff; border-radius: 16px; padding: 8px 0; margin-bottom: 16px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
      .menu-item { display: flex; align-items: center; padding: 16px 20px; border-bottom: 1px solid #f5f5f5; cursor: pointer; transition: background 0.2s; }
      .menu-item:last-child { border-bottom: none; }
      .menu-item:hover { background: #fafafa; }
      .menu-icon { width: 40px; height: 40px; border-radius: 10px; background: #f0f0f5; display: flex; align-items: center; justify-content: center; margin-right: 14px; font-size: 18px; }
      .menu-text { flex: 1; font-size: 15px; color: #1a1a1a; }
      .menu-arrow { color: #c7c7cc; font-size: 14px; }

      /* Book Button */
      .book-btn { display: block; width: 100%; padding: 16px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #fff; border: none; border-radius: 12px; font-size: 16px; font-weight: 600; cursor: pointer; margin-bottom: 16px; box-shadow: 0 4px 16px rgba(102, 126, 234, 0.35); }
      .book-btn:active { transform: scale(0.98); }

      /* Modal (generic) */
      .modal { display: none; position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); z-index: 100; align-items: flex-end; justify-content: center; }
      .modal.active { display: flex; }
      .modal-content { background: #fff; border-radius: 20px 20px 0 0; width: 100%; max-width: 375px; max-height: 80vh; overflow-y: auto; padding: 24px; animation: slideUp 0.3s ease; }
      @keyframes slideUp { from { transform: translateY(100%); } to { transform: translateY(0); } }
      .modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
      .modal-title { font-size: 18px; font-weight: 600; }
      .modal-close { width: 32px; height: 32px; border-radius: 50%; background: #f5f5f5; display: flex; align-items: center; justify-content: center; cursor: pointer; font-size: 18px; color: #666; }
      .time-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-bottom: 20px; }
      .time-slot { padding: 12px; text-align: center; border: 1px solid #e5e5e5; border-radius: 8px; font-size: 14px; cursor: pointer; transition: all 0.2s; }
      .time-slot.selected { background: #667eea; color: #fff; border-color: #667eea; }
      .time-slot:active { transform: scale(0.95); }
      .confirm-book { width: 100%; padding: 16px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #fff; border: none; border-radius: 12px; font-size: 16px; font-weight: 600; cursor: pointer; }

      /* Footer */
      .footer { text-align: center; font-size: 11px; color: #8e8e93; line-height: 1.6; }

      /* Login Overlay */
      .overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 20px; }
      .overlay.hidden { display: none; }
      .login-card { background: #fff; border-radius: 16px; padding: 32px 24px; width: 100%; max-width: 360px; box-shadow: 0 8px 32px rgba(0,0,0,0.3); }
      .login-card h2 { font-size: 22px; color: #1a1a1a; margin-bottom: 8px; text-align: center; }
      .login-card .subtitle { font-size: 13px; color: #8e8e93; text-align: center; margin-bottom: 24px; }
      .login-input { width: 100%; padding: 14px 16px; border: 1px solid #e5e5e5; border-radius: 10px; font-size: 15px; margin-bottom: 12px; outline: none; transition: border 0.2s; }
      .login-input:focus { border-color: #667eea; }
      .login-btn { width: 100%; padding: 14px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #fff; border: none; border-radius: 10px; font-size: 16px; font-weight: 600; cursor: pointer; margin-top: 8px; }
      .login-btn:disabled { opacity: 0.5; cursor: not-allowed; }
      .login-error { color: #e74c3c; font-size: 13px; text-align: center; min-height: 18px; margin-top: 8px; }
      .shop-picker { margin-top: 16px; }
      .shop-picker-title { font-size: 13px; color: #8e8e93; margin-bottom: 10px; text-align: center; }
      .shop-item { padding: 12px 16px; border: 1px solid #e5e5e5; border-radius: 10px; margin-bottom: 8px; cursor: pointer; transition: all 0.2s; }
      .shop-item:hover { border-color: #667eea; background: #fafafa; }

      /* Transactions list */
      .tx-list { margin-top: 8px; }
      .tx-item { display: flex; justify-content: space-between; align-items: center; padding: 14px 0; border-bottom: 1px solid #f5f5f5; }
      .tx-item:last-child { border-bottom: none; }
      .tx-left { display: flex; flex-direction: column; gap: 4px; }
      .tx-type { font-size: 14px; font-weight: 500; color: #1a1a1a; }
      .tx-time { font-size: 12px; color: #8e8e93; }
      .tx-remark { font-size: 12px; color: #666; margin-top: 2px; }
      .tx-right { text-align: right; }
      .tx-amount { font-size: 16px; font-weight: 600; }
      .tx-amount.positive { color: #4A9D5B; }
      .tx-amount.negative { color: #e74c3c; }
      .tx-amount::before { content: '+'; }
      .tx-amount.negative::before { content: '-'; }
      .tx-balance-after { font-size: 11px; color: #8e8e93; margin-top: 2px; }
      .tx-empty { text-align: center; padding: 40px 20px; color: #8e8e93; font-size: 14px; }
      .load-more-btn { width: 100%; padding: 12px; background: #f5f5f7; color: #1a1a1a; border: none; border-radius: 10px; font-size: 14px; cursor: pointer; margin-top: 16px; }
      .load-more-btn:disabled { opacity: 0.5; cursor: not-allowed; }
  </style>
</head>
<body>
<!-- Login Overlay -->
<div id="loginOverlay" class="overlay">
  <div class="login-card">
    <h2>会员登录</h2>
    <p class="subtitle">输入手机号与姓名即可查询会员信息</p>
    <input id="loginPhone" class="login-input" placeholder="手机号" maxlength="11" inputmode="numeric" />
    <input id="loginName"  class="login-input" placeholder="姓名"   maxlength="20" />
    <div id="shopPicker" class="shop-picker" style="display:none">
      <p class="shop-picker-title">检测到您在多家店铺有会员，请选择：</p>
      <div id="shopList"></div>
    </div>
    <button id="loginSubmit" class="login-btn" onclick="onLoginSubmit()">登录</button>
    <div id="loginError" class="login-error"></div>
  </div>
</div>

<div class="container" id="mainContainer" style="display:none">
  <!-- Profile Card -->
  <div class="profile-card">
    <div class="profile-header">
      <div class="avatar" id="avatar">?</div>
      <div class="profile-info">
        <h1 id="memberName">--</h1>
        <p id="memberPhone">--</p>
        <div class="store-badge" id="storeBadge">-- · 会员</div>
      </div>
    </div>
  </div>

  <!-- Balance Card -->
  <div class="balance-card">
    <div class="balance-label">账户余额</div>
    <div class="balance-amount" id="balanceAmount">0.00</div>
    <div class="balance-hint" id="balanceHint">--</div>
  </div>

  <!-- Menu -->
  <div class="menu-section">
    <div class="menu-item" onclick="openTxModal()">
      <div class="menu-icon">💳</div>
      <span class="menu-text">消费记录</span>
      <span class="menu-arrow">›</span>
    </div>
    <div class="menu-item">
      <div class="menu-icon">🎁</div>
      <span class="menu-text">优惠券</span>
      <span class="menu-arrow">›</span>
    </div>
    <div class="menu-item" onclick="onLogout()">
      <div class="menu-icon">⚙️</div>
      <span class="menu-text">退出登录</span>
      <span class="menu-arrow">›</span>
    </div>
  </div>

  <!-- Book Button -->
  <button class="book-btn" onclick="openModal()">📅 立即预约理发</button>

  <!-- Footer -->
  <div class="footer">
    会员卡仅限持卡人本人使用<br>
    最终解释权归店铺所有
  </div>
</div>

<!-- Booking Modal -->
<div class="modal" id="bookingModal">
  <div class="modal-content">
    <div class="modal-header">
      <span class="modal-title">选择预约时间</span>
      <div class="modal-close" onclick="closeModal()">✕</div>
    </div>
    <div class="time-grid">
      <div class="time-slot">09:00</div>
      <div class="time-slot">10:00</div>
      <div class="time-slot">11:00</div>
      <div class="time-slot selected">14:00</div>
      <div class="time-slot">15:00</div>
      <div class="time-slot">16:00</div>
      <div class="time-slot">17:00</div>
      <div class="time-slot">18:00</div>
      <div class="time-slot">19:00</div>
    </div>
    <button class="confirm-book" onclick="confirmBooking()">确认预约</button>
  </div>
</div>

<!-- Transactions Modal -->
<div class="modal" id="txModal">
  <div class="modal-content">
    <div class="modal-header">
      <span class="modal-title">消费记录</span>
      <div class="modal-close" onclick="closeTxModal()">✕</div>
    </div>
    <div id="txList" class="tx-list"></div>
    <button id="loadMoreBtn" class="load-more-btn" onclick="loadMoreTx()" style="display:none">加载更多</button>
  </div>
</div>

<script>
    // ====== 配置 ======
    const API_BASE = '/barbershop';
    const TOKEN_KEY = 'member_token';
    const ID_KEY    = 'member_id';
    const NAME_KEY  = 'member_name';
    const SHOP_KEY  = 'member_shop_id';
    const SHOPNAME_KEY = 'member_shop_name';

    // ====== 工具 ======
    async function apiCall(url, method, body, opts) {
        opts = opts || {};
        const headers = { 'Content-Type': 'application/json' };
        const token = localStorage.getItem(TOKEN_KEY);
        if (token) headers['Authorization'] = 'Bearer ' + token;
        const res = await fetch(API_BASE + url, {
            method: method,
            headers: headers,
            body: body ? JSON.stringify(body) : undefined,
        });
        const text = await res.text();
        let json;
        try { json = text ? JSON.parse(text) : {}; } catch (e) { throw new Error('服务异常'); }
        if (opts.skip401 && (json.code === 401 || res.status === 401)) {
            logout();
            throw new Error('未登录');
        }
        if (json.code !== 0 && json.code !== 20000) {
            throw new Error(json.message || '请求失败');
        }
        return json.data;
    }

    function logout() {
        [TOKEN_KEY, ID_KEY, NAME_KEY, SHOP_KEY, SHOPNAME_KEY].forEach(k => localStorage.removeItem(k));
        location.reload();
    }

    function onLogout() {
        if (confirm('确定退出登录？')) logout();
    }

    function maskPhone(phone) {
        if (!phone || phone.length < 11) return phone || '--';
        return phone.slice(0, 3) + '****' + phone.slice(7);
    }

    function avatarChar(name) {
        if (!name) return '?';
        return name.charAt(0).toUpperCase();
    }

    function fmtMoney(n) {
        const num = Number(n);
        if (isNaN(num)) return '0.00';
        return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }

    function fmtDate(s) {
        if (!s) return '';
        return s.length >= 10 ? s.slice(0, 10) : s;
    }

    // ====== 登录流程 ======
    let matchedShops = [];
    let chosenShopId = null;

    async function onLoginSubmit() {
        const phone = document.getElementById('loginPhone').value.trim();
        const name  = document.getElementById('loginName').value.trim();
        const errEl = document.getElementById('loginError');
        const btn   = document.getElementById('loginSubmit');
        errEl.textContent = '';
        if (!phone || !name) { errEl.textContent = '请填写手机号和姓名'; return; }
        btn.disabled = true;
        btn.textContent = '查询中...';
        try {
            const list = await apiCall('/member/login/match?phone=' + encodeURIComponent(phone) + '&name=' + encodeURIComponent(name), 'GET', null, { skip401: true });
            if (!list || list.length === 0) {
                errEl.textContent = '未找到匹配的会员，请联系门店';
                return;
            }
            if (list.length === 1) {
                await doLogin(phone, name, list[0].shopId, list[0].shopName, list[0].memberId);
                return;
            }
            // 多店匹配,展示店铺选择
            matchedShops = list;
            chosenShopId = null;
            const picker = document.getElementById('shopPicker');
            const listEl = document.getElementById('shopList');
            listEl.innerHTML = '';
            list.forEach(s => {
                const div = document.createElement('div');
                div.className = 'shop-item';
                div.textContent = s.shopName + '（会员 #' + s.memberId + '）';
                div.onclick = () => {
                    chosenShopId = s.shopId;
                    document.querySelectorAll('.shop-item').forEach(el => el.style.borderColor = '#e5e5e5');
                    div.style.borderColor = '#667eea';
                    btn.disabled = false;
                    btn.textContent = '使用此店铺登录';
                    // 立即登录
                    doLogin(phone, name, s.shopId, s.shopName, s.memberId);
                };
                listEl.appendChild(div);
            });
            picker.style.display = 'block';
            btn.disabled = true;
            btn.textContent = '请选择店铺';
        } catch (e) {
            errEl.textContent = e.message || '查询失败';
        } finally {
            if (!chosenShopId) {
                btn.disabled = false;
                if (btn.textContent === '查询中...') btn.textContent = '登录';
            }
        }
    }

    async function doLogin(phone, name, shopId, shopName, expectedMemberId) {
        const errEl = document.getElementById('loginError');
        const btn   = document.getElementById('loginSubmit');
        try {
            const data = await apiCall('/member/login', 'POST', { phone, name, shopId });
            if (expectedMemberId && data.id !== expectedMemberId) {
                errEl.textContent = '店铺信息不一致，请重试';
                return;
            }
            localStorage.setItem(TOKEN_KEY, data.token);
            localStorage.setItem(ID_KEY, data.id);
            localStorage.setItem(NAME_KEY, data.username);
            localStorage.setItem(SHOP_KEY, shopId);
            localStorage.setItem(SHOPNAME_KEY, shopName || '');
            enterMain();
        } catch (e) {
            errEl.textContent = e.message || '登录失败';
            btn.disabled = false;
            btn.textContent = '登录';
        }
    }

    function enterMain() {
        document.getElementById('loginOverlay').classList.add('hidden');
        document.getElementById('mainContainer').style.display = 'block';
        loadProfile();
    }

    // ====== 主页数据加载 ======
    async function loadProfile() {
        const memberId = localStorage.getItem(ID_KEY);
        const shopId   = localStorage.getItem(SHOP_KEY);
        if (!memberId || !shopId) { logout(); return; }

        const shopName = localStorage.getItem(SHOPNAME_KEY);
        if (shopName) {
            document.getElementById('storeBadge').textContent = shopName + ' · 会员';
        }

        try {
            const member = await apiCall('/member/' + memberId + '?shopId=' + shopId, 'GET');
            document.getElementById('memberName').textContent  = member.name;
            document.getElementById('memberPhone').textContent = maskPhone(member.phone);
            document.getElementById('avatar').textContent      = avatarChar(member.name);
            if (shopName) {
                document.getElementById('storeBadge').textContent = shopName + ' · 会员';
            }
            document.getElementById('balanceAmount').textContent = fmtMoney(member.balance);
        } catch (e) {
            if (/未登录|无效token/.test(e.message)) logout();
            else alert('加载会员资料失败：' + e.message);
        }

        try {
            const balance = await apiCall('/member/' + memberId + '/balance', 'GET');
            document.getElementById('balanceAmount').textContent = fmtMoney(balance);
        } catch (e) { /* 已在 loadProfile 中提示 */ }

        try {
            const pageData = await apiCall('/member/' + memberId + '/transactions/page?page=1&size=5', 'GET');
            if (pageData && pageData.records && pageData.records.length > 0) {
                document.getElementById('balanceHint').textContent = '最近消费: ' + fmtDate(pageData.records[0].createdTime);
            } else {
                document.getElementById('balanceHint').textContent = '暂无消费记录';
            }
        } catch (e) { /* 已在 loadProfile 中提示 */ }
    }

    // ====== 交易记录 Modal ======
    let txPage = 1;
    let txHasMore = false;
    const TX_SIZE = 20;

    async function openTxModal() {
        document.getElementById('txModal').classList.add('active');
        document.getElementById('txList').innerHTML = '';
        txPage = 1;
        await loadTxPage(txPage, false);
    }

    function closeTxModal() {
        document.getElementById('txModal').classList.remove('active');
    }

    async function loadTxPage(page, append) {
        const memberId = localStorage.getItem(ID_KEY);
        const btn = document.getElementById('loadMoreBtn');
        try {
            const data = await apiCall('/member/' + memberId + '/transactions/page?page=' + page + '&size=' + TX_SIZE, 'GET');
            txHasMore = data.hasMore;
            renderTxList(data.records, append);
            if (txHasMore) {
                btn.style.display = 'block';
                btn.disabled = false;
                btn.textContent = '加载更多';
            } else {
                btn.style.display = 'block';
                btn.disabled = true;
                btn.textContent = '已加载全部';
            }
        } catch (e) {
            alert('加载交易记录失败：' + e.message);
        }
    }

    function renderTxList(records, append) {
        const listEl = document.getElementById('txList');
        if (!append) listEl.innerHTML = '';
        if (!records || records.length === 0) {
            listEl.innerHTML = '<div class="tx-empty">暂无交易记录</div>';
            return;
        }
        records.forEach(r => {
            const div = document.createElement('div');
            div.className = 'tx-item';
            const isConsume = r.type === 'CONSUME';
            const signClass = isConsume ? 'negative' : 'positive';
            const sign = isConsume ? '-' : '+';
            const typeLabel = isConsume ? '消费' : '储值';
            div.innerHTML =
                '<div class="tx-left">' +
                  '<span class="tx-type">' + typeLabel + '</span>' +
                  '<span class="tx-time">' + fmtDate(r.createdTime) + '</span>' +
                  (r.remark ? '<span class="tx-remark">' + r.remark + '</span>' : '') +
                '</div>' +
                '<div class="tx-right">' +
                  '<div class="tx-amount ' + signClass + '">¥' + fmtMoney(r.amount) + '</div>' +
                  '<div class="tx-balance-after">余额 ¥' + fmtMoney(r.balanceAfter) + '</div>' +
                '</div>';
            listEl.appendChild(div);
        });
    }

    function loadMoreTx() {
        const btn = document.getElementById('loadMoreBtn');
        btn.disabled = true;
        btn.textContent = '加载中...';
        txPage++;
        loadTxPage(txPage, true);
    }

    // ====== 预约 Modal（保留原有占位逻辑） ======
    function openModal() { document.getElementById('bookingModal').classList.add('active'); }
    function closeModal() { document.getElementById('bookingModal').classList.remove('active'); }
    function confirmBooking() { alert('预约成功！'); closeModal(); }

    // ====== 初始化 ======
    document.querySelectorAll('.time-slot').forEach(slot => {
        slot.addEventListener('click', function() {
            document.querySelectorAll('.time-slot').forEach(s => s.classList.remove('selected'));
            this.classList.add('selected');
        });
    });

    (function init() {
        const token = localStorage.getItem(TOKEN_KEY);
        const id    = localStorage.getItem(ID_KEY);
        const shop  = localStorage.getItem(SHOP_KEY);
        if (token && id && shop) {
            enterMain();
        } else {
            document.getElementById('loginOverlay').classList.remove('hidden');
            document.getElementById('mainContainer').style.display = 'none';
        }
    })();
</script>
</body>
</html>
```

- [ ] **Step 2: 手工浏览器测试**

启动后端（`mvn spring-boot:run`），浏览器打开 `frontend/member-portal.html`：

1. **未登录态**：直接看到登录蒙层，主页面隐藏。
2. **唯一匹配**：输入匹配的手机号 + 姓名 → 直接登录 → 进入主页 → 数据正确渲染（姓名、手机号脱敏、店铺徽章、余额、最近消费日期）。
3. **无匹配**：输入不存在的 → 显示"未找到匹配的会员"。
4. **多店匹配**（数据库准备）：输入相同手机号 + 姓名 → 显示店铺选择 → 选店 → 进入对应店铺主页。
5. **二次访问**：登录后刷新页面 → 直接进入主页（无登录蒙层）。
6. **消费记录 Modal**：点击"消费记录" → 显示列表 + "加载更多"按钮（交易 < 20 时显示"已加载全部"且禁用）。
7. **退出登录**：点击"退出登录"菜单 → 确认 → 回到登录蒙层。

- [ ] **Step 3: 提交**

```bash
git add frontend/member-portal.html
git commit -m "feat(portal): 会员门户对接后端,新增登录/数据回填/消费记录"
```

---

## 自检清单（实施后）

- [ ] 后端编译通过：`mvn compile -q` BUILD SUCCESS
- [ ] 后端启动后 swagger 能看到 3 个新接口
- [ ] `/member/login/match` 跨店铺查询正确
- [ ] `/member/login` 返回 JWT,subject 为 `member:{id}`
- [ ] `/member/{id}/transactions/page` 分页正确,`hasMore` 标志正确
- [ ] 任意 `/member/{otherId}/...` 用自己的 member token 访问被 403 拒绝
- [ ] 前端登录蒙层 / 主页 / 交易 Modal 三态切换流畅
- [ ] 退出登录后刷新回到登录蒙层