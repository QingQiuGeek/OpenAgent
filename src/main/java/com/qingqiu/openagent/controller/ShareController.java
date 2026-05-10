package com.qingqiu.openagent.controller;

import com.qingqiu.openagent.model.common.R;
import com.qingqiu.openagent.model.request.CreateShareLinkRequest;
import com.qingqiu.openagent.model.vo.ShareLinkVO;
import com.qingqiu.openagent.model.vo.ShareSnapshotVO;
import com.qingqiu.openagent.service.ShareLinkFacadeService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 14:30
 * @description: Share controller —— /api/share 鉴权；/api/share/public/* 由 SaTokenInterceptor 白名单
 */
@RestController
@RequestMapping("/api/share")
@AllArgsConstructor
public class ShareController {

    private final ShareLinkFacadeService shareLinkFacadeService;

    /** 创建分享 */
    @PostMapping
    public R<ShareLinkVO> create(@RequestBody CreateShareLinkRequest request) {
        return R.success(shareLinkFacadeService.create(request));
    }

    /** 我的分享列表 */
    @GetMapping("/my")
    public R<List<ShareLinkVO>> myLinks() {
        return R.success(shareLinkFacadeService.myLinks());
    }

    /** 撤销分享 */
    @DeleteMapping("/{shareId}")
    public R<Boolean> revoke(@PathVariable("shareId") String shareId) {
        shareLinkFacadeService.revoke(shareId);
        return R.success(true);
    }

    /** 公开访问：通过 slug 拿快照（无需登录） */
    @GetMapping("/public/{slug}")
    public R<ShareSnapshotVO> viewBySlug(@PathVariable("slug") String slug) {
        return R.success(shareLinkFacadeService.viewBySlug(slug));
    }
}
