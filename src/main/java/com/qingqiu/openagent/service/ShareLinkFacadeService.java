package com.qingqiu.openagent.service;

import com.qingqiu.openagent.model.request.CreateShareLinkRequest;
import com.qingqiu.openagent.model.vo.ShareLinkVO;
import com.qingqiu.openagent.model.vo.ShareSnapshotVO;
import java.util.List;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 14:06
 * @description: ShareLink facade service
 */
public interface ShareLinkFacadeService {

    /** 创建分享链接，返回 slug + id */
    ShareLinkVO create(CreateShareLinkRequest request);

    /** 当前用户的分享列表 */
    List<ShareLinkVO> myLinks();

    /** 撤销分享（软删除） */
    void revoke(String shareId);

    /** 公开访问：通过 slug 拿快照（自动 view_count++）。过期或不存在抛 NOT_FOUND。 */
    ShareSnapshotVO viewBySlug(String slug);
}
