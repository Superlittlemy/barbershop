package com.slm.barbershop.config;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.validation.Valid;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * MyBatis-Plus IPage 分页参数处理器
 * <p>
 * 在 {@link org.springframework.web.servlet.config.annotation.WebMvcConfigurer} 里注册后生效。
 * <p>
 * 支持：
 * <ul>
 *   <li>原生 {@link Page} 及其子类（如 {@code BillQuery extends Page<Bill>}）</li>
 *   <li>前端传 {@code current} / {@code size} / {@code sort} 等白名单字段</li>
 *   <li>排序字段名接受驼峰或下划线，方向接受 asc/desc（缺省 desc）</li>
 *   <li>大小写不敏感的字段名匹配</li>
 * </ul>
 * <p>
 * 安全策略：
 * <ul>
 *   <li>Page 父类内部细节（optimizeCountSql / countId / maxLimit / orders 等）不被前端覆盖</li>
 *   <li>size 有上限保护（默认 10，上限 1000）</li>
 *   <li>current 必须 >= 1</li>
 * </ul>
 */
@Slf4j
public class PageHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String SORT_PARAM = "sort";
    private static final String ORDER_ASC = "asc";
    private static final String ORDER_DESC = "desc";
    private static final String DEFAULT_SORT_FIELD = "created_time";
    private static final long DEFAULT_SIZE = 10L;
    private static final long MAX_SIZE = 1000L;

    /** Page 父类允许前端控制的字段 */
    private static final Set<String> PAGE_ALLOWED = Set.of("current", "size", "searchCount");

    /** 反射字段缓存：Class -> 该类所有非 static 字段名（含整条父类链） */
    private static final ConcurrentMap<Class<?>, Set<String>> FIELD_CACHE = new ConcurrentHashMap<>();

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return Page.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Page<?> resolveArgument(MethodParameter parameter,
                                    ModelAndViewContainer mavContainer,
                                    NativeWebRequest webRequest,
                                    WebDataBinderFactory binderFactory) throws Exception {
        Class<?> pageType = parameter.getParameterType();

        // 1. 反射创建子类实例（支持 BillQuery extends Page<Bill>）
        Page<?> page = (Page<?>) pageType.getDeclaredConstructor().newInstance();

        // 2. 解析 Page<T> 的 T（用作排序字段白名单）
        Class<?> entityClass = ResolvableType.forMethodParameter(parameter)
                .as(Page.class).getGeneric(0).resolve();
        if (entityClass == null) {
            throw new IllegalArgumentException("无法解析分页泛型类型: " + parameter);
        }

        // 3. 收集可绑定字段 = 子类字段（含父类链）∪ Page 父类白名单 ∪ sort
        Set<String> allowed = new HashSet<>(getAllFieldNames(pageType));
        allowed.addAll(PAGE_ALLOWED);
        allowed.add(SORT_PARAM);

        Map<String, Object> bindable = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> e : webRequest.getParameterMap().entrySet()) {
            String key = e.getKey();
            if (!allowed.contains(key)) continue;
            String[] values = e.getValue();
            if (values == null || values.length == 0) continue;
            bindable.put(key, values.length == 1 ? values[0] : values);
        }

        // 4. 用 binderFactory 绑定到子类实例（type conversion 走 Spring ConversionService）
        WebDataBinder binder = binderFactory.createBinder(webRequest, page, pageType.getSimpleName());
        binder.bind(new MutablePropertyValues(bindable));
        if (binder.getBindingResult().hasErrors()) {
            log.warn("分页参数绑定失败 [{}]: {}", pageType.getSimpleName(),
                    binder.getBindingResult().getAllErrors());
        }

        // 4.5. JSR-303 校验：触发参数上的 @Valid / @Validated 级联校验
        validateIfApplicable(binder, parameter);
        if (binder.getBindingResult().hasErrors()) {
            throw new BindException(binder.getBindingResult());
        }

        // 5. 边界保护（Page#getCurrent / getSize 返回 long 基本类型，默认 0）
        if (page.getCurrent() < 1) {
            page.setCurrent(1L);
        }
        if (page.getSize() < 1) {
            page.setSize(DEFAULT_SIZE);
        }
        if (page.getSize() > MAX_SIZE) {
            log.debug("size {} 超过上限 {}，已截断", page.getSize(), MAX_SIZE);
            page.setSize(MAX_SIZE);
        }

        // 6. 排序
        handleSort(page, webRequest.getParameter(SORT_PARAM), entityClass);
        return page;
    }

    /**
     * 检查参数上是否标注了 {@link Valid} 或 {@link Validated}，
     * 若有则触发 JSR-303 级联校验（落入 {@link org.springframework.validation.SmartValidator}，
     * 通常是 {@code LocalValidatorFactoryBean}）。
     */
    private void validateIfApplicable(WebDataBinder binder, MethodParameter parameter) {
        for (Annotation ann : parameter.getParameterAnnotations()) {
            if (ann instanceof Valid || ann instanceof Validated) {
                Object hints = AnnotationUtils.getValue(ann);
                Object[] hintArray = (hints instanceof Object[]) ? (Object[]) hints : new Object[]{hints};
                binder.validate(hintArray);
                return;
            }
        }
    }

    /**
     * 缓存反射：pageType 自己 + 整条父类链
     */
    private Set<String> getAllFieldNames(Class<?> pageType) {
        return FIELD_CACHE.computeIfAbsent(pageType, clazz -> {
            Set<String> names = new HashSet<>();
            Class<?> cur = clazz;
            while (cur != null && cur != Object.class) {
                for (Field f : cur.getDeclaredFields()) {
                    if (!Modifier.isStatic(f.getModifiers())) {
                        names.add(f.getName());
                    }
                }
                cur = cur.getSuperclass();
            }
            return names;
        });
    }

    /**
     * 处理排序参数
     * <p>协议：{@code ?sort=field1,asc&sort=field2,desc} 或 {@code ?sort=field1,asc,field2,desc}
     */
    private void handleSort(Page<?> page, String sort, Class<?> entityClass) {
        if (StringUtils.isBlank(sort)) {
            page.addOrder(OrderItem.desc(DEFAULT_SORT_FIELD));
            return;
        }

        TableInfo info = TableInfoHelper.getTableInfo(entityClass);
        if (info == null) {
            log.warn("未找到实体 {} 的 TableInfo，跳过排序", entityClass.getName());
            return;
        }

        ListIterator<String> it = Arrays.asList(sort.split(",")).listIterator();
        while (it.hasNext()) {
            String field = it.next();
            String column = resolveColumn(info, field);
            if (column == null) {
                log.warn("非实体排序字段: {}", field);
                continue;
            }

            OrderItem item;
            if (it.hasNext()) {
                String dir = it.next();
                if (ORDER_ASC.equalsIgnoreCase(dir)) {
                    item = OrderItem.asc(column);
                } else if (ORDER_DESC.equalsIgnoreCase(dir)) {
                    item = OrderItem.desc(column);
                } else {
                    // 非 asc/desc：当前字段按 desc，下一个再当字段处理
                    item = OrderItem.desc(column);
                    it.previous();
                }
            } else {
                item = OrderItem.desc(column);
            }
            page.addOrder(item);
        }
    }

    /**
     * 解析字段名对应的真实列名
     * <p>先匹配普通字段（fieldList），再匹配主键（fieldList 不含主键）
     */
    private String resolveColumn(TableInfo info, String field) {
        return info.getFieldList().stream()
                .filter(t -> t.getProperty().equalsIgnoreCase(field)
                          || t.getColumn().equalsIgnoreCase(field))
                .map(TableFieldInfo::getColumn)
                .findFirst()
                .orElseGet(() -> {
                    String keyProperty = info.getKeyProperty();
                    if (keyProperty == null) return null;
                    if (keyProperty.equalsIgnoreCase(field)
                            || info.getKeyColumn().equalsIgnoreCase(field)) {
                        return info.getKeyColumn();
                    }
                    return null;
                });
    }
}
