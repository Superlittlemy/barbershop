package com.slm.barbershop.config;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.core.MethodParameter;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * MyBatis IPage 分页参数处理器<p>
 *
 * PS：该配置需要添加到 {@link org.springframework.web.servlet.config.annotation.WebMvcConfigurer } 参数解析器才会生效
 */
@Slf4j
public class PageHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String PAGE_PARAM = "current";
    private static final long PAGE_DEFAULT = 0;
    private static final String SIZE_PARAM = "pageSize";
    private static final long SIZE_DEFAULT = 10;
    private static final String SORT_PARAM = "sort";
    public static final String ORDER_ASC = "asc";
    public static final String ORDER_DESC = "desc";
    public static final String DEFAULT_SORT_FIELD = "created_time";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return IPage.class.equals(parameter.getParameterType());
    }

    @Override
    public IPage<?> resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        // 构建Page
        Page<?> page = this.generatePage(webRequest.getParameter(PAGE_PARAM), webRequest.getParameter(SIZE_PARAM));
        // 排序
        String sort = webRequest.getParameter(SORT_PARAM);
        // 解析分页实体类型
        Class<?> clazz = (Class<?>) ((ParameterizedType) parameter.getGenericParameterType()).getActualTypeArguments()[0];
        this.handleSort(page, StringUtils.isNotBlank(sort) ? Arrays.asList(sort.split(",")) : List.of(), clazz);
        return page;
    }

    /**
     * 构建Page
     *
     * @param current 当前页数
     * @param pageSize 分页大小
     * @return Page对象
     */
    public Page<?> generatePage(String current, String pageSize) {
        return Page.of(
                NumberUtils.isParsable(current) ? Long.parseLong(current) : PAGE_DEFAULT,
                NumberUtils.isParsable(pageSize) ? Long.parseLong(pageSize) : SIZE_DEFAULT
        );
    }

    /**
     * 处理分页参数
     * 分页参数格式：field[,acs|desc]
     *
     * @param page 分页对象
     * @param sort 排序参数
     */
    public void handleSort(Page<?> page, List<String> sort, Class<?> clazz) {
        if (!CollectionUtils.isEmpty(sort)) {
            ListIterator<String> iterator = sort.listIterator();
            // 获取实体全部字段（约定两层：业务属性+基础属性）
            Field[] declaredFields = clazz.getDeclaredFields();
            Field[] superClassDeclaredFields = clazz.getSuperclass().getDeclaredFields();
            List<String> fields = Stream.concat(Arrays.stream(declaredFields), Arrays.stream(superClassDeclaredFields)).map(Field::getName).collect(Collectors.toList());
            while (iterator.hasNext()) {
                String field = iterator.next();
                // 匹配字段
                if (fields.contains(field)) {
                    // 转表字段
                    String tableField = field.replaceAll("[A-Z]", "_$0").toLowerCase();
                    if (iterator.hasNext()) {
                        // 下一个可能是排序标识
                        String maybeSort = iterator.next();
                        if (List.of(ORDER_ASC, ORDER_DESC).contains(maybeSort)) {
                            // 按指定方向排序
                            page.addOrder(ORDER_ASC.equals(maybeSort) ? OrderItem.asc(tableField) : OrderItem.desc(tableField));
                        } else {
                            page.addOrder(OrderItem.desc(tableField));
                            // 非排序回退
                            iterator.previous();
                        }
                    } else {
                        // 最后一个
                        page.addOrder(OrderItem.desc(tableField));
                    }
                } else {
                    log.warn("非实体排序字段 {}", field);
                }
            }
        } else {
            page.addOrder(OrderItem.desc(DEFAULT_SORT_FIELD));
        }
    }

}
