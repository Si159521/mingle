'use client';

import { useEffect, useState } from 'react';
import { useSocket } from '@/hooks/useSocket';
import { ChatMessagePayload } from '@/features/chat/common/types/ChatMessagePayload';
import { ChatRoomType } from '@/features/chat/common/types/ChatRoomType';
import { MessageFormat } from '@/features/chat/common/types/MessageFormat';
import { fetchDmChatMessages } from './fetchDmChatMessages'; // 과거 메시지 fetch 함수

export function useDmChat(roomId: number, receiverId: number | null) {
  const [messages, setMessages] = useState<ChatMessagePayload[]>([]); // 과거 + 실시간 메시지 포함

  const token =
    typeof window !== 'undefined' ? sessionStorage.getItem('token') : null; // SSR 안전하게 localStorage 접근

  const userId =
    typeof window !== 'undefined'
      ? Number(sessionStorage.getItem('userId'))
      : 0; // senderId도 SSR-safe로

  // WebSocket 연결 및 메시지 핸들링 (token이 있을 때만)
  const { send } = useSocket(roomId, token, (msg) => {
    if (msg.chatType === ChatRoomType.DIRECT && msg.roomId === roomId) {
      setMessages((prev) => [...prev, msg]);
    }
  });

  // 초기에 과거 메시지 불러오기
  useEffect(() => {
    const loadMessages = async () => {
      try {
        const pastMessages = await fetchDmChatMessages(roomId);
        setMessages(pastMessages); // 과거 메시지 저장
      } catch (error) {
        console.error('과거 메시지 불러오기 실패:', error);
      }
    };

    loadMessages();
  }, [roomId]);

  // 메시지 전송 시 sendFn이 준비되었는지 확인
  const sendDmMessage = (content: string) => {
    if (receiverId === null) return;

    const payload: ChatMessagePayload = {
      roomId,
      receiverId,
      senderId: userId,
      content,
      format: MessageFormat.TEXT,
      chatType: ChatRoomType.DIRECT,
      createdAt: new Date().toISOString(),
    };
    send(payload); // sendFn이 준비되었으면 사용
  };

  return {
    messages,
    sendDmMessage,
  };
}
