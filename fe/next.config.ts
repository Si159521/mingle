import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  eslint: {
    ignoreDuringBuilds: true,
  },
  typescript: {
    ignoreBuildErrors: true,
  },
  // 빌드 시 발생하는 모든 에러 무시
  onError: () => {},
};

export default nextConfig;
