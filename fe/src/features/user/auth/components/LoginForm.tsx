'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { apiClient } from '@/lib/api/apiClient'; // lib에서 불러오기

export default function LoginForm() {
  const [loginId, setLoginId] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const router = useRouter();

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      // 로그인 요청 및 응답 구조 반영 (token 포함)
      const res = await apiClient<{
        accessToken: string;
        userId: number;
        nickname: string;
        email: string;
      }>('/users/login', {
        method: 'POST',
        body: JSON.stringify({ loginId, password }),
      });

      // 로그인 전 로컬스토리지 초기화
      sessionStorage.clear();

      // 응답에서 토큰 및 사용자 정보 저장
      sessionStorage.setItem('token', res.accessToken);
      sessionStorage.setItem('userId', res.userId.toString());
      sessionStorage.setItem('nickname', res.nickname);
      sessionStorage.setItem('email', res.email);

      // 로그인 성공 후 페이지 이동
      router.replace('/schedule');
    } catch (error) {
      // 실패 시 에러 메시지 설정
      console.error('로그인 에러:', error);
      setError('로그인에 실패했습니다. 아이디 또는 비밀번호를 확인하세요.');
    }
  };

  return (
    <form onSubmit={handleLogin} className="space-y-4 w-full">
      <div>
        <label className="block text-xs mb-1 text-[#4b2563]">아이디</label>
        <input
          type="text"
          value={loginId}
          onChange={(e) => setLoginId(e.target.value)}
          placeholder="아이디를 입력하세요"
          className="w-full border rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#a084e8]"
          required
        />
      </div>
      <div>
        <label className="block text-xs mb-1 text-[#4b2563]">비밀번호</label>
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="비밀번호를 입력하세요"
          className="w-full border rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#a084e8]"
          required
        />
      </div>
      {error && <p className="text-red-500 text-sm mt-1">{error}</p>}
      <button
        type="submit"
        className="w-full bg-[#a084e8] text-white py-2 rounded font-bold hover:bg-[#7c5fd3] transition"
      >
        로그인
      </button>
    </form>
  );
}
