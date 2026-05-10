import { get, post, del, BASE_URL } from "./http.ts";

export interface ShareLinkVO {
  id: string;
  slug: string;
  sessionId: string;
  expireAt?: string;
  viewCount: number;
  createdAt?: string;
}

export interface ShareSnapshotMessage {
  id: string;
  role: "user" | "assistant" | "tool" | "system";
  content?: string;
  metadata?: Record<string, unknown>;
  createdAt?: string;
}

export interface ShareSnapshotVO {
  slug: string;
  title?: string;
  agentName?: string;
  messages: ShareSnapshotMessage[];
  createdAt?: string;
  viewCount?: number;
}

export interface CreateShareLinkRequest {
  sessionId: string;
  /** null/0 表示永不过期 */
  expireDays?: number | null;
}

export async function createShareLink(body: CreateShareLinkRequest): Promise<ShareLinkVO> {
  return post<ShareLinkVO>("/api/share", body);
}

export async function getMyShareLinks(): Promise<ShareLinkVO[]> {
  return get<ShareLinkVO[]>("/api/share/my");
}

export async function revokeShareLink(shareId: string): Promise<boolean> {
  return del<boolean>(`/api/share/${encodeURIComponent(shareId)}`);
}

/** 公开接口，无需登录；http.ts 默认带 cookie，但后端在白名单内 */
export async function viewSharePublic(slug: string): Promise<ShareSnapshotVO> {
  return get<ShareSnapshotVO>(`/api/share/public/${encodeURIComponent(slug)}`, undefined, {
    skipAuth: true,
  });
}

/** 拼出公开访问 URL（前端路由），用户复制使用 */
export function buildShareUrl(slug: string): string {
  const origin = typeof window !== "undefined" ? window.location.origin : BASE_URL;
  return `${origin}/share/${slug}`;
}
