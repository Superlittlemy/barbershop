package com.slm.common.model;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 统一分页响应包装
 * <p>
 * 替换原 {@code IPage} 直接序列化 + 各业务自定义 {@code *PageVO} 的双轨制。
 * 前端协议：{@code data.records / data.total / data.current / data.size / data.pages}。
 *
 * @param <T> 单条数据类型（一般是 Response/VO，不是 Entity）
 */
@Data
@NoArgsConstructor
@Schema(description = "分页响应")
public class PageResult<T> {

    @Schema(description = "当前页数据")
    private List<T> records;

    @Schema(description = "总记录数")
    private long total;

    @Schema(description = "当前页(从1开始)")
    private long current;

    @Schema(description = "每页大小")
    private long size;

    @Schema(description = "总页数")
    private long pages;

    @Schema(description = "是否还有更多")
    private Boolean hasMore;

    /**
     * 记录同构的转换：IPage<S> → PageResult<T>，元素 1-to-1 转换
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> r = new PageResult<>();
        r.setRecords(page.getRecords());
        r.setTotal(page.getTotal());
        r.setCurrent(page.getCurrent());
        r.setSize(page.getSize());
        r.setPages(page.getPages());
        r.setHasMore(computeHasMore(page));
        return r;
    }

    /**
     * 记录已经转换好：IPage<S> + List<T> → PageResult<T>
     * <p>用于 service 内部已经把 Entity 转成 Response/VO 的场景
     */
    public static <S, T> PageResult<T> of(IPage<S> source, List<T> records) {
        PageResult<T> r = new PageResult<>();
        r.setRecords(records);
        r.setTotal(source.getTotal());
        r.setCurrent(source.getCurrent());
        r.setSize(source.getSize());
        r.setPages(source.getPages());
        r.setHasMore(computeHasMore(source));
        return r;
    }

    /**
     * 一站式：IPage<S> 经 mapper 转换得到 PageResult<T>
     */
    public static <S, T> PageResult<T> map(IPage<S> source, Function<S, T> mapper) {
        List<T> mapped = source.getRecords() == null ? Collections.emptyList()
                : source.getRecords().stream().map(mapper).collect(Collectors.toList());
        return of(source, mapped);
    }

    public static <T> PageResult<T> empty(long current, long size) {
        PageResult<T> r = new PageResult<>();
        r.setRecords(Collections.emptyList());
        r.setTotal(0L);
        r.setCurrent(current);
        r.setSize(size);
        r.setPages(0L);
        r.setHasMore(false);
        return r;
    }

    private static boolean computeHasMore(IPage<?> p) {
        return p.getCurrent() * p.getSize() < p.getTotal();
    }
}
