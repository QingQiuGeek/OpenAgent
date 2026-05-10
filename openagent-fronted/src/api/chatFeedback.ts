import { get, post, del } from "./http.ts";

export interface ChatFeedbackVO {
  id: number;
  messageId: string;
  rating: 1 | -1;
  reasonTags?: string[];
  comment?: string;
  createdAt?: string;
}

export interface SubmitFeedbackRequest {
  messageId: string;
  rating: 1 | -1;
  reasonTags?: string[];
  comment?: string;
}

export async function submitFeedback(body: SubmitFeedbackRequest): Promise<number> {
  return post<number>("/api/chat-feedback", body);
}

export async function withdrawFeedback(messageId: string): Promise<boolean> {
  return del<boolean>(`/api/chat-feedback/${encodeURIComponent(messageId)}`);
}

export async function getFeedback(messageId: string): Promise<ChatFeedbackVO | null> {
  return get<ChatFeedbackVO | null>(`/api/chat-feedback/${encodeURIComponent(messageId)}`);
}

export async function batchGetFeedback(messageIds: string[]): Promise<ChatFeedbackVO[]> {
  if (!messageIds.length) return [];
  return get<ChatFeedbackVO[]>(
    `/api/chat-feedback?messageIds=${messageIds.map(encodeURIComponent).join(",")}`
  );
}
