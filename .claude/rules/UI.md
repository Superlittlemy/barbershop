---
name: UI 标准规范
description: 适用于中后台管理系统的表格/列表UI规范，涵盖对齐、空状态、文本截断、交互事件和布局约束
---

# UI 标准规范

## 1. 列对齐规则
**适用场景**：表格列、列表项列

| 数据类型 | 对齐方式 | 示例字段 |
|---------|---------|---------|
| 金额 | 右对齐 | `amount`, `price`, `total` |
| 数值（计数/百分比） | 右对齐 | `count`, `rate`, `percentage` |
| 普通文本 | 左对齐 | `name`, `description`, `status` |
| 操作列 | 固定宽度，居中对齐 | `actions`, `operations` |

**实现方式**：使用 CSS `text-align` 属性，金额/数值列额外加 `font-variant-numeric: tabular-nums` 保持数字等宽。

---

## 2. 空数据展示
**规则**：
- 通用兜底文案：`"暂无数据"`
- 业务场景专用文案：需按上下文自定义，例如交易列表用 `"暂无交易记录"`，订单列表用 `"暂无订单"`

**实现方式**：表格 `empty` 插槽或独立空状态组件，禁止返回 `null` 或空数组时不渲染任何内容。

---

## 3. 长文本截断（单行省略）
**规则**：
- 仅对**主列**（如名称、标题）启用截断，非所有列
- 截断后需支持 **hover 查看完整文本**（用 `title` 属性或 Tooltip 组件）

**CSS 实现**：
```css
.ellipsis-cell {
    max-width: <按业务设定，如 200px>;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}
```

---

## 4. 弹窗（Modal）交互规则
**规则**：
- **禁止点击弹窗外空白区域（overlay / backdrop）关闭弹窗**
- 关闭弹窗的唯二合法途径：
    1. 弹窗**右上角的关闭按钮**（`BaseModal` 内置 `.modal-close`）
    2. 弹窗**内部 footer / body 内的关闭按钮**（如「取消」「关闭」「确定」等）
- ESC 键行为由具体业务决定，默认不绑定；如需支持，需在组件内显式监听 `keydown.esc` 并自行处理

**实现方式**：
- 统一通过 [BaseModal.vue](../../src/components/ui/BaseModal.vue) 实现，组件的 `closeOnBackdrop` prop **默认值已设为 `false`**
- 调用方**不需要**再显式传 `:close-on-backdrop="false"`，直接使用 `<BaseModal>` 即可
- 除非有明确的特殊场景（如系统级危险确认弹窗），否则不要把 `closeOnBackdrop` 改为 `true`

**为什么**：
- 防止用户误触：表单填写到一半时点击外部导致输入丢失
- 防止数据丢失：异步请求进行中（如 `submitting`）被误关闭导致状态错乱
- 行为统一：所有弹窗关闭路径一致，降低用户认知负担

---

## ✅ 自检清单（代码生成后检查）
- [ ] 金额/数字列是否右对齐并使用了 `tabular-nums`？
- [ ] 空状态是否显示了正确的文案（业务场景 vs 通用兜底）？
- [ ] 主列是否添加了省略号截断 + hover 查看？
- [ ] 行内按钮是否调用了 `stopPropagation()`？
- [ ] 表格是否整体不滚动，仅表体区域滚动？
- [ ] 分页栏是否固定在底部、不随内容滚动？
- [ ] 新增/修改的 Modal 是否依赖 `BaseModal` 的默认行为（**未传** `:close-on-backdrop="true"`）？