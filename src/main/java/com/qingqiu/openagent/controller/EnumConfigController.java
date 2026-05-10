package com.qingqiu.openagent.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.qingqiu.openagent.model.common.R;
import com.qingqiu.openagent.model.entity.EnumConfig;
import com.qingqiu.openagent.service.EnumConfigFacadeService;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 09:40
 * @description: EnumConfig controller
 */
@RestController
@RequestMapping("/api/enums")
@AllArgsConstructor
public class EnumConfigController {

    private final EnumConfigFacadeService enumConfigFacadeService;

    /** 获取某类别下未禁用的枚举值列表（前端下拉用）。 */
    @GetMapping("/{type}")
    public R<List<String>> listValues(@PathVariable("type") String type) {
        return R.success(enumConfigFacadeService.listValues(type));
    }

    /** 列出指定类别下全部项（含禁用），管理员维护用。 */
    @SaCheckLogin
    @GetMapping("/{type}/all")
    public R<List<EnumConfig>> listAll(@PathVariable("type") String type) {
        return R.success(enumConfigFacadeService.listAll(type));
    }

    /** 新增一项枚举值（管理员）。 */
    @SaCheckLogin
    @PostMapping("/{type}")
    public R<Long> add(@PathVariable("type") String type, @RequestBody AddEnumRequest body) {
        return R.success(enumConfigFacadeService.add(type, body == null ? null : body.getValue()));
    }

    /** 启用 / 禁用某项（管理员）。 */
    @SaCheckLogin
    @PutMapping("/{id}/status")
    public R<Boolean> updateStatus(@PathVariable("id") Long id, @RequestBody UpdateStatusRequest body) {
        enumConfigFacadeService.updateStatus(id, body == null ? null : body.getStatus());
        return R.success(true);
    }

    @Data
    public static class AddEnumRequest {
        private String value;
    }

    @Data
    public static class UpdateStatusRequest {
        private Integer status;
    }
}
