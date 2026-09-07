package com.slm.barbershop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slm.barbershop.converter.EmployeeConverter;
import com.slm.barbershop.entity.Employee;
import com.slm.barbershop.entity.EmployeeServiceItem;
import com.slm.barbershop.entity.ServiceItem;
import com.slm.barbershop.exception.BizException;
import com.slm.barbershop.mapper.EmployeeMapper;
import com.slm.barbershop.mapper.EmployeeServiceItemMapper;
import com.slm.barbershop.mapper.ServiceItemMapper;
import com.slm.barbershop.model.EmployeeOptionVO;
import com.slm.barbershop.model.EmployeeQuery;
import com.slm.barbershop.model.EmployeeRequest;
import com.slm.barbershop.model.EmployeeResponse;
import com.slm.barbershop.model.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EmployeeService extends ServiceImpl<EmployeeMapper, Employee> {

    /** 工号生成冲突的最大重试次数(并发下 uk_shop_employee_no 兜底) */
    private static final int EMPLOYEE_NO_MAX_RETRY = 3;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private EmployeeServiceItemMapper employeeServiceItemMapper;

    @Autowired
    private ServiceItemMapper serviceItemMapper;

    @Autowired
    private EmployeeConverter employeeConverter;

    @Transactional(rollbackFor = Exception.class)
    public Employee create(EmployeeRequest request) {
        validateServiceItems(request.getShopId(), request.getServiceItemIds());
        Employee employee = employeeConverter.toEntity(request);
        insertWithEmployeeNo(request.getShopId(), employee);
        saveServiceItems(employee.getId(), request.getServiceItemIds());
        return employee;
    }

    /**
     * 软删员工:先清空 employee_no 释放 uk_shop_employee_no 占位,再 removeById 软删。
     * 避免"两个软删员工同工号"导致后续创建触发唯一键冲突。
     *
     * 实现细节:
     * - updateById 默认 UpdateStrategy.NOT_NULL 会吞掉 entity 字段值,null 不生成 SET 子句;
     *   所以 employeeNo=null 必须通过 wrapper.set(...) 显式指定。
     * - 复用 selectById 拿到的 employee 作为 update entity:非空 metaObject 触发
     *   EntityMetadataConfig.updateFill 写入 updatedBy/updatedTime;wrapper 的 set
     *   优先于 entity 同名字段,因此 entity 上原有的 employeeNo 不会污染清号 SQL。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Employee employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "员工不存在");
        }
        // 关联项目一并清理
        employeeServiceItemMapper.delete(
                new LambdaQueryWrapper<EmployeeServiceItem>()
                        .eq(EmployeeServiceItem::getEmployeeId, id));
        // 清号:wrapper.set 显式清掉 employeeNo(绕开 UpdateStrategy.NOT_NULL);
        // employee 实体用于承载 MetaObjectHandler 自动填充 updatedBy/updatedTime。
        employeeMapper.update(employee,
                new LambdaUpdateWrapper<Employee>()
                        .eq(Employee::getId, id)
                        .set(Employee::getEmployeeNo, null));
        removeById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Employee update(Long id, EmployeeRequest request) {
        Employee employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "员工不存在");
        }
        employeeConverter.updateEntity(employee, request);
        employeeMapper.updateById(employee);
        // 关联项目全删全插,归属校验以员工实际店铺为准
        employeeServiceItemMapper.delete(
                new LambdaQueryWrapper<EmployeeServiceItem>()
                        .eq(EmployeeServiceItem::getEmployeeId, id));
        validateServiceItems(employee.getShopId(), request.getServiceItemIds());
        saveServiceItems(id, request.getServiceItemIds());
        return employee;
    }

    public EmployeeResponse getDetail(Long shopId, Long id) {
        if (shopId == null || id == null) {
            throw new BizException(HttpStatus.BAD_REQUEST, "参数不能为空");
        }
        Employee employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "员工不存在");
        }
        if (!employee.getShopId().equals(shopId)) {
            throw new BizException(HttpStatus.FORBIDDEN, "员工不属于当前店铺");
        }
        return toResponseWithItems(employee);
    }

    /**
     * 分页查询某店铺员工;keyword 模糊匹配姓名或工号。
     * serviceItemIds / serviceItemNames 在 service 层批量回填,避免 N+1。
     */
    public PageResult<EmployeeResponse> page(EmployeeQuery query) {
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<Employee>()
                .eq(Employee::getShopId, query.getShopId());
        String keyword = query.getKeyword();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Employee::getName, keyword)
                    .or()
                    .like(Employee::getEmployeeNo, keyword));
        }
        IPage<Employee> result = employeeMapper.selectPage(query, wrapper);
        return PageResult.of(result, toResponseListWithItems(result.getRecords()));
    }

    /**
     * 员工选项列表(门户预约下拉用):仅 id / employeeNo / name,按工号升序。
     */
    public List<EmployeeOptionVO> listOptions(Long shopId) {
        if (shopId == null) {
            throw new BizException(HttpStatus.BAD_REQUEST, "shopId 不能为空");
        }
        List<Employee> employees = employeeMapper.selectList(
                new LambdaQueryWrapper<Employee>()
                        .eq(Employee::getShopId, shopId)
                        .orderByAsc(Employee::getEmployeeNo));
        return employees.stream().map(e -> {
            EmployeeOptionVO vo = new EmployeeOptionVO();
            vo.setId(e.getId());
            vo.setEmployeeNo(e.getEmployeeNo());
            vo.setName(e.getName());
            return vo;
        }).collect(Collectors.toList());
    }

    public EmployeeResponse toResponseWithItems(Employee employee) {
        if (employee == null) {
            return null;
        }
        return toResponseListWithItems(Collections.singletonList(employee)).get(0);
    }

    // ====== 私有辅助 ======

    /**
     * 生成工号并插入;并发下唯一键冲突时重新取号重试。
     */
    private void insertWithEmployeeNo(Long shopId, Employee employee) {
        for (int attempt = 1; ; attempt++) {
            employee.setEmployeeNo(generateEmployeeNo(shopId));
            try {
                employeeMapper.insert(employee);
                return;
            } catch (DuplicateKeyException e) {
                if (attempt >= EMPLOYEE_NO_MAX_RETRY) {
                    throw new BizException(HttpStatus.CONFLICT, "工号生成冲突,请重试");
                }
            }
        }
    }

    /**
     * 店内工号递增生成:E + 4 位数字,从 E0001 起(超过 9999 自然升位)。
     * 取店内当前最大工号 +1;软删员工的工号视为已释放(@TableLogic 过滤),可被复用。
     */
    private String generateEmployeeNo(Long shopId) {
        Employee max = employeeMapper.selectOne(
                new LambdaQueryWrapper<Employee>()
                        .eq(Employee::getShopId, shopId)
                        .orderByDesc(Employee::getEmployeeNo)
                        .last("LIMIT 1"));
        long next = 1;
        if (max != null && max.getEmployeeNo() != null && max.getEmployeeNo().length() > 1) {
            next = Long.parseLong(max.getEmployeeNo().substring(1)) + 1;
        }
        return String.format("E%04d", next);
    }

    /**
     * 校验服务项目全部存在且属于当前店铺(可空:允许不选)。
     */
    private void validateServiceItems(Long shopId, List<Long> serviceItemIds) {
        if (serviceItemIds == null || serviceItemIds.isEmpty()) {
            return;
        }
        Set<Long> distinctIds = new HashSet<>(serviceItemIds);
        List<ServiceItem> matched = serviceItemMapper.selectList(
                new LambdaQueryWrapper<ServiceItem>()
                        .eq(ServiceItem::getShopId, shopId)
                        .in(ServiceItem::getId, distinctIds));
        if (matched.size() != distinctIds.size()) {
            throw new BizException(HttpStatus.BAD_REQUEST, "服务项目不存在或不属于当前店铺");
        }
    }

    private void saveServiceItems(Long employeeId, List<Long> serviceItemIds) {
        if (serviceItemIds == null || serviceItemIds.isEmpty()) {
            return;
        }
        for (Long itemId : new HashSet<>(serviceItemIds)) {
            EmployeeServiceItem rel = new EmployeeServiceItem();
            rel.setEmployeeId(employeeId);
            rel.setServiceItemId(itemId);
            rel.setCreatedTime(LocalDateTime.now());
            employeeServiceItemMapper.insert(rel);
        }
    }

    /**
     * 批量实体转 response,并一次性回填 serviceItemIds / serviceItemNames(固定 2 次批量查询)。
     */
    private List<EmployeeResponse> toResponseListWithItems(List<Employee> employees) {
        if (employees == null || employees.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> employeeIds = employees.stream().map(Employee::getId).collect(Collectors.toList());
        List<EmployeeServiceItem> relations = employeeServiceItemMapper.selectList(
                new LambdaQueryWrapper<EmployeeServiceItem>()
                        .in(EmployeeServiceItem::getEmployeeId, employeeIds));
        Map<Long, List<Long>> itemIdsByEmployeeId = new HashMap<>();
        for (EmployeeServiceItem rel : relations) {
            itemIdsByEmployeeId.computeIfAbsent(rel.getEmployeeId(), k -> new ArrayList<>())
                    .add(rel.getServiceItemId());
        }
        Set<Long> itemIds = relations.stream()
                .map(EmployeeServiceItem::getServiceItemId)
                .collect(Collectors.toSet());
        Map<Long, String> nameMap = itemIds.isEmpty() ? Collections.emptyMap()
                : serviceItemMapper.selectBatchIds(itemIds).stream()
                    .collect(Collectors.toMap(ServiceItem::getId, ServiceItem::getName));
        List<EmployeeResponse> out = new ArrayList<>(employees.size());
        for (Employee employee : employees) {
            EmployeeResponse resp = employeeConverter.toResponse(employee);
            List<Long> itemIdsOfEmployee = itemIdsByEmployeeId.getOrDefault(employee.getId(), Collections.emptyList());
            resp.setServiceItemIds(itemIdsOfEmployee);
            resp.setServiceItemNames(itemIdsOfEmployee.stream()
                    .map(nameMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
            out.add(resp);
        }
        return out;
    }

}
