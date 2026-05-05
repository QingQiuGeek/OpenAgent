import { message } from "antd";

// API 响应类型定义，匹配后端 ApiResponse 结构
export interface ApiResponse<T = unknown> {
  code: number;
  message: string;
  data: T;
}

// 请求配置选项
export interface RequestOptions extends RequestInit {
  params?: Record<string, string | number | boolean | null | undefined>;
  skipAuth?: boolean;
}

// 后端服务根地址。各业务模块前缀各异（/api、/user、/sse），在调用处显式拼。
export const BASE_URL = "http://localhost:8080";

// 后端 BizExceptionEnum.NOT_LOGIN_ERROR
export const NOT_LOGIN_CODE = 40200;

/**
 * 构建完整的 URL（包含查询参数）
 */
function buildUrl(url: string, params?: Record<string, string | number | boolean | null | undefined>): string {
  const fullUrl = `${BASE_URL}${url}`;
  
  if (!params || Object.keys(params).length === 0) {
    return fullUrl;
  }

  const searchParams = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== null && value !== undefined) {
      searchParams.append(key, String(value));
    }
  });

  const queryString = searchParams.toString();
  return queryString ? `${fullUrl}?${queryString}` : fullUrl;
}

/**
 * 触发"未登录"事件，由 AuthContext 监听并清 token + 弹登录框。
 */
function emitUnauthorized() {
  window.dispatchEvent(new CustomEvent("auth:unauthorized"));
}

/**
 * 处理响应
 */
async function handleResponse<T>(response: Response): Promise<ApiResponse<T>> {
  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  const data: ApiResponse<T> = await response.json();

  // 未登录 → 触发全局事件（cookie 由后端清，前端不再本地存 token）
  if (data.code === NOT_LOGIN_CODE) {
    emitUnauthorized();
    throw new Error(data.message || "未登录");
  }

  // 其他业务错误
  if (data.code !== 200) {
    message.error(data.message || "请求失败");
    throw new Error(data.message || "请求失败");
  }

  return data;
}

/**
 * 封装的 fetch 请求函数
 */
async function request<T = unknown>(
  url: string,
  options: RequestOptions = {}
): Promise<T> {
  const { params, headers, skipAuth: _skipAuth, ...restOptions } = options;

  const fullUrl = buildUrl(url, params);

  const mergedHeaders: Record<string, string> = {
    "Content-Type": "application/json",
    ...(headers as Record<string, string> | undefined),
  };

  try {
    const response = await fetch(fullUrl, {
      ...restOptions,
      headers: mergedHeaders,
      // 由浏览器自动带 sa-token cookie
      credentials: "include",
    });

    const apiResponse = await handleResponse<T>(response);
    return apiResponse.data;
  } catch (error) {
    if (error instanceof Error) {
      throw error;
    }
    throw new Error("网络请求失败");
  }
}

/**
 * GET 请求
 */
export function get<T = unknown>(
  url: string,
  params?: Record<string, string | number | boolean | null | undefined>,
  options?: Omit<RequestOptions, "method" | "body" | "params">
): Promise<T> {
  return request<T>(url, {
    ...options,
    method: "GET",
    params,
  });
}

/**
 * POST 请求
 */
export function post<T = unknown>(
  url: string,
  data?: unknown,
  options?: Omit<RequestOptions, "method" | "body">
): Promise<T> {
  return request<T>(url, {
    ...options,
    method: "POST",
    body: data ? JSON.stringify(data) : undefined,
  });
}

/**
 * PUT 请求
 */
export function put<T = unknown>(
  url: string,
  data?: unknown,
  options?: Omit<RequestOptions, "method" | "body">
): Promise<T> {
  return request<T>(url, {
    ...options,
    method: "PUT",
    body: data ? JSON.stringify(data) : undefined,
  });
}

/**
 * PATCH 请求
 */
export function patch<T = unknown>(
  url: string,
  data?: unknown,
  options?: Omit<RequestOptions, "method" | "body">
): Promise<T> {
  return request<T>(url, {
    ...options,
    method: "PATCH",
    body: data ? JSON.stringify(data) : undefined,
  });
}

/**
 * DELETE 请求
 */
export function del<T = unknown>(
  url: string,
  params?: Record<string, string | number | boolean | null | undefined>,
  options?: Omit<RequestOptions, "method" | "body" | "params">
): Promise<T> {
  return request<T>(url, {
    ...options,
    method: "DELETE",
    params,
  });
}

// 导出默认对象，方便使用
export default {
  get,
  post,
  put,
  patch,
  delete: del,
};
