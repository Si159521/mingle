import React from "react";

interface GoodsSortBarProps {
  value: string;
  onChange: (value: string) => void;
}

const sortOptions = [
  { label: "신상품순", value: "createdAt,desc" },
  { label: "가격 낮은순", value: "itemPrice,asc" },
  { label: "가격 높은순", value: "itemPrice,desc" },
];

const GoodsSortBar: React.FC<GoodsSortBarProps> = ({ value, onChange }) => {
  return (
    <div className="flex items-center gap-2">
      <label htmlFor="goods-sort" className="text-xs font-medium text-gray-700">
        정렬
      </label>
      <select
        id="goods-sort"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="px-2 py-1.5 text-sm border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
      >
        {sortOptions.map((opt) => (
          <option key={opt.value} value={opt.value}>
            {opt.label}
          </option>
        ))}
      </select>
    </div>
  );
};

export default GoodsSortBar;
