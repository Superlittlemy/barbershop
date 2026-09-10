package com.slm.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 通配扫描 com.slm.*.mapper 子包下的所有接口并注册为 MyBatis Mapper Bean。
 * <p>
 * 解决问题：{@code @MapperScan("com.slm")} 会误把 {@code com.slm.barbershop.lock.LockStrategy}
 * 这类非 Mapper 接口当成 Mapper，导致启动期 {@code Invalid bound statement (not found)}。
 * <p>
 * 约定：本项目所有 Mapper 接口必须放在 {@code com.slm.<module>.mapper} 包下；
 * 新增模块只要遵循命名约定，无需在此处追加配置。
 */
@Slf4j
public class WildcardMapperScanRegistrar implements ImportBeanDefinitionRegistrar {

    private static final String CLASS_PATH_PATTERN = "classpath*:com/slm/*/mapper/**/*.class";
    private static final String PACKAGE_PREFIX = "com.slm.";

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        Set<String> mapperPackages = discoverMapperPackages();
        if (mapperPackages.isEmpty()) {
            log.warn("WildcardMapperScanRegistrar: 未发现 com.slm.*.mapper 子包");
            return;
        }
        log.info("WildcardMapperScanRegistrar 扫描到的 Mapper 包: {}", mapperPackages);

        for (String mapperPackage : mapperPackages) {
            registerMapperScanner(registry, mapperPackage);
        }
    }

    /**
     * 为单个 mapper 包注册一个 MapperScannerConfigurer Bean 定义。
     * MapperScannerConfigurer 与 @MapperScan 行为等价,在 afterPropertiesSet 阶段执行扫描,
     * 时序正确,能拿到 Spring Boot 自动配置的 SqlSessionFactory。
     */
    private void registerMapperScanner(BeanDefinitionRegistry registry, String mapperPackage) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
                org.mybatis.spring.mapper.MapperScannerConfigurer.class);
        builder.addPropertyValue("basePackage", mapperPackage);
        // 通过 BeanName 引用,避免硬依赖 SqlSessionFactory Bean 已实例化
        builder.addPropertyValue("sqlSessionFactoryBeanName", "sqlSessionFactory");
        registry.registerBeanDefinition(mapperPackage + ".MapperScannerConfigurer#" + System.nanoTime(),
                builder.getBeanDefinition());
    }

    /**
     * 扫描 classpath 中 com.slm.*.mapper 子包路径并去重。
     */
    private Set<String> discoverMapperPackages() {
        Set<String> packages = new LinkedHashSet<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        CachingMetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(resolver);
        try {
            Resource[] resources = resolver.getResources(CLASS_PATH_PATTERN);
            for (Resource resource : resources) {
                if (!resource.isReadable()) {
                    continue;
                }
                MetadataReader reader = readerFactory.getMetadataReader(resource);
                if (!reader.getClassMetadata().isInterface()) {
                    continue;
                }
                String className = reader.getClassMetadata().getClassName();
                if (!className.startsWith(PACKAGE_PREFIX)) {
                    continue;
                }
                int mapperIdx = className.lastIndexOf(".mapper.");
                if (mapperIdx < 0) {
                    continue;
                }
                String mapperPackage = className.substring(0, mapperIdx + ".mapper".length());
                packages.add(mapperPackage);
            }
        } catch (Exception e) {
            throw new IllegalStateException("扫描 com.slm.*.mapper 子包失败", e);
        }
        return packages;
    }

}

