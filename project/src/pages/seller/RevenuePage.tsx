import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { DollarSign, TrendingUp, Calendar, Download } from 'lucide-react';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import { Card } from '../../components/ui';
import { formatPrice } from '../../lib/utils';

const monthlyRevenueData = [
  { month: 'Jan', revenue: 120000, orders: 45 },
  { month: 'Feb', revenue: 145000, orders: 52 },
  { month: 'Mar', revenue: 132000, orders: 48 },
  { month: 'Apr', revenue: 168000, orders: 61 },
  { month: 'May', revenue: 185000, orders: 68 },
  { month: 'Jun', revenue: 142000, orders: 51 },
];

export default function RevenuePage() {
  const [timeRange, setTimeRange] = useState('6m');

  const totalRevenue = monthlyRevenueData.reduce((sum, m) => sum + m.revenue, 0);
  const totalOrders = monthlyRevenueData.reduce((sum, m) => sum + m.orders, 0);
  const avgOrderValue = totalRevenue / totalOrders;

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Revenue & Payouts</h1>
          <p className="text-gray-500 dark:text-gray-400">
            Track your earnings and payout history
          </p>
        </div>
        <div className="flex items-center gap-2">
          <select
            value={timeRange}
            onChange={(e) => setTimeRange(e.target.value)}
            className="px-4 py-2 rounded-lg border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
          >
            <option value="1m">Last 1 month</option>
            <option value="3m">Last 3 months</option>
            <option value="6m">Last 6 months</option>
            <option value="1y">Last 1 year</option>
          </select>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
          <Card>
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400">Total Revenue</p>
                <p className="text-3xl font-bold text-gray-900 dark:text-white mt-1">
                  {formatPrice(totalRevenue)}
                </p>
                <div className="flex items-center gap-1 mt-2">
                  <TrendingUp className="w-4 h-4 text-emerald-500" />
                  <span className="text-emerald-600 text-sm font-medium">+12.5%</span>
                </div>
              </div>
              <div className="w-12 h-12 bg-emerald-100 dark:bg-emerald-900/30 rounded-xl flex items-center justify-center">
                <DollarSign className="w-6 h-6 text-emerald-600" />
              </div>
            </div>
          </Card>
        </motion.div>

        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}>
          <Card>
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400">Total Orders</p>
                <p className="text-3xl font-bold text-gray-900 dark:text-white mt-1">
                  {totalOrders}
                </p>
                <div className="flex items-center gap-1 mt-2">
                  <TrendingUp className="w-4 h-4 text-emerald-500" />
                  <span className="text-emerald-600 text-sm font-medium">+8.3%</span>
                </div>
              </div>
              <div className="w-12 h-12 bg-blue-100 dark:bg-blue-900/30 rounded-xl flex items-center justify-center">
                <Calendar className="w-6 h-6 text-blue-600" />
              </div>
            </div>
          </Card>
        </motion.div>

        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.2 }}>
          <Card>
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400">Avg Order Value</p>
                <p className="text-3xl font-bold text-gray-900 dark:text-white mt-1">
                  {formatPrice(avgOrderValue)}
                </p>
                <div className="flex items-center gap-1 mt-2">
                  <TrendingUp className="w-4 h-4 text-emerald-500" />
                  <span className="text-emerald-600 text-sm font-medium">+5.2%</span>
                </div>
              </div>
              <div className="w-12 h-12 bg-purple-100 dark:bg-purple-900/30 rounded-xl flex items-center justify-center">
                <DollarSign className="w-6 h-6 text-purple-600" />
              </div>
            </div>
          </Card>
        </motion.div>
      </div>

      <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.3 }}>
        <Card>
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
              Monthly Revenue
            </h2>
            <button className="flex items-center gap-2 text-emerald-600 hover:text-emerald-700 text-sm">
              <Download className="w-4 h-4" />
              Export
            </button>
          </div>
          <div className="h-80">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={monthlyRevenueData}>
                <defs>
                  <linearGradient id="revenueGradient" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#10B981" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="#10B981" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
                <XAxis dataKey="month" stroke="#9CA3AF" fontSize={12} />
                <YAxis stroke="#9CA3AF" fontSize={12} />
                <Tooltip
                  formatter={(value: number) => formatPrice(value)}
                  contentStyle={{
                    backgroundColor: '#1F2937',
                    border: '1px solid #374151',
                    borderRadius: '8px',
                    color: '#F9FAFB',
                  }}
                />
                <Area
                  type="monotone"
                  dataKey="revenue"
                  stroke="#10B981"
                  strokeWidth={2}
                  fill="url(#revenueGradient)"
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </Card>
      </motion.div>

      <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.4 }}>
        <Card>
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
            Recent Payouts
          </h2>
          <div className="space-y-3">
            <div className="flex items-center justify-between p-4 bg-gray-50 dark:bg-gray-800 rounded-lg">
              <div>
                <p className="font-medium text-gray-900 dark:text-white">Payout #P-2024-0012</p>
                <p className="text-sm text-gray-500 dark:text-gray-400">June 1, 2024</p>
              </div>
              <div className="text-right">
                <p className="font-semibold text-gray-900 dark:text-white">{formatPrice(85000)}</p>
                <span className="text-xs text-emerald-600 bg-emerald-100 dark:bg-emerald-900/30 px-2 py-1 rounded">
                  Completed
                </span>
              </div>
            </div>
            <div className="flex items-center justify-between p-4 bg-gray-50 dark:bg-gray-800 rounded-lg">
              <div>
                <p className="font-medium text-gray-900 dark:text-white">Payout #P-2024-0011</p>
                <p className="text-sm text-gray-500 dark:text-gray-400">May 15, 2024</p>
              </div>
              <div className="text-right">
                <p className="font-semibold text-gray-900 dark:text-white">{formatPrice(72000)}</p>
                <span className="text-xs text-emerald-600 bg-emerald-100 dark:bg-emerald-900/30 px-2 py-1 rounded">
                  Completed
                </span>
              </div>
            </div>
          </div>
        </Card>
      </motion.div>
    </div>
  );
}
