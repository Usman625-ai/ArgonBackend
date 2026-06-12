import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import {
  Users,
  Package,
  ShoppingCart,
  DollarSign,
  TrendingUp,
  TrendingDown,
  Calendar,
} from 'lucide-react';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  BarChart,
  Bar,
} from 'recharts';
import api from '../../lib/api';
import type { DashboardStats, ApiResponse } from '../../types';
import { Card, CardSkeleton } from '../../components/ui';
import { formatPrice } from '../../lib/utils';
import { useAppSelector } from '../../store';

const dailyRevenueData = [
  { date: 'Jun 1', revenue: 45000 },
  { date: 'Jun 2', revenue: 52000 },
  { date: 'Jun 3', revenue: 48000 },
  { date: 'Jun 4', revenue: 61000 },
  { date: 'Jun 5', revenue: 55000 },
  { date: 'Jun 6', revenue: 67000 },
  { date: 'Jun 7', revenue: 72000 },
  { date: 'Jun 8', revenue: 58000 },
  { date: 'Jun 9', revenue: 63000 },
  { date: 'Jun 10', revenue: 71000 },
];

const ordersByStatusData = [
  { status: 'PENDING', count: 45, color: '#F59E0B' },
  { status: 'PROCESSING', count: 32, color: '#3B82F6' },
  { status: 'SHIPPED', count: 28, color: '#8B5CF6' },
  { status: 'DELIVERED', count: 156, color: '#10B981' },
  { status: 'CANCELLED', count: 12, color: '#EF4444' },
];

const statCards = [
  { icon: Users, label: 'Total Sellers', key: 'totalSellers', color: 'from-blue-500 to-indigo-600' },
  { icon: Users, label: 'Total Customers', key: 'totalCustomers', color: 'from-purple-500 to-pink-600' },
  { icon: Package, label: 'Total Products', key: 'totalProducts', color: 'from-orange-500 to-red-600' },
  { icon: ShoppingCart, label: 'Total Orders', key: 'totalOrders', color: 'from-emerald-500 to-teal-600' },
];

export default function AdminDashboard() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const { user } = useAppSelector((state) => state.auth);

  useEffect(() => {
    fetchStats();
  }, []);

  const fetchStats = async () => {
    try {
      const response = await api.get<ApiResponse<DashboardStats>>('/api/admin/dashboard/stats');
      setStats(response.data.data);
    } catch (error) {
      console.error('Failed to fetch stats:', error);
    } finally {
      setLoading(false);
    }
  };

  const container = {
    hidden: { opacity: 0 },
    show: {
      opacity: 1,
      transition: { staggerChildren: 0.1 },
    },
  };

  const item = {
    hidden: { opacity: 0, y: 20 },
    show: { opacity: 1, y: 0 },
  };

  return (
    <div className="space-y-8">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
            Welcome back, {user?.name || 'Admin'}!
          </h1>
          <p className="text-gray-500 dark:text-gray-400">
            Here's what's happening with your platform today.
          </p>
        </div>
        <div className="flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400">
          <Calendar className="w-4 h-4" />
          {new Date().toLocaleDateString('en-US', {
            weekday: 'long',
            year: 'numeric',
            month: 'long',
            day: 'numeric',
          })}
        </div>
      </div>

      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {[...Array(4)].map((_, i) => (
            <CardSkeleton key={i} />
          ))}
        </div>
      ) : (
        <motion.div
          variants={container}
          initial="hidden"
          animate="show"
          className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6"
        >
          {statCards.map((card) => (
            <motion.div key={card.key} variants={item}>
              <Card className="relative overflow-hidden">
                <div className={`absolute top-0 right-0 w-32 h-32 bg-gradient-to-br ${card.color} opacity-10 rounded-full -translate-y-1/2 translate-x-1/2`} />
                <div className="relative">
                  <div className={`inline-flex p-3 rounded-xl bg-gradient-to-br ${card.color} mb-4`}>
                    <card.icon className="w-6 h-6 text-white" />
                  </div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">{card.label}</p>
                  <p className="text-3xl font-bold text-gray-900 dark:text-white mt-1">
                    {stats?.[card.key as keyof DashboardStats]?.toLocaleString() || 0}
                  </p>
                </div>
              </Card>
            </motion.div>
          ))}
        </motion.div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
          className="lg:col-span-2"
        >
          <Card>
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
                Revenue Overview
              </h2>
              <div className="flex items-center gap-2">
                <TrendingUp className="w-5 h-5 text-emerald-500" />
                <span className="text-emerald-600 text-sm font-medium">+12.5%</span>
              </div>
            </div>
            <div className="grid grid-cols-3 gap-4 mb-6">
              <div className="p-4 bg-gray-50 dark:bg-gray-800 rounded-xl">
                <p className="text-sm text-gray-500 dark:text-gray-400">Today</p>
                <p className="text-xl font-bold text-gray-900 dark:text-white mt-1">
                  {formatPrice(stats?.todayRevenue || 0)}
                </p>
              </div>
              <div className="p-4 bg-gray-50 dark:bg-gray-800 rounded-xl">
                <p className="text-sm text-gray-500 dark:text-gray-400">This Month</p>
                <p className="text-xl font-bold text-gray-900 dark:text-white mt-1">
                  {formatPrice(stats?.monthRevenue || 0)}
                </p>
              </div>
              <div className="p-4 bg-gray-50 dark:bg-gray-800 rounded-xl">
                <p className="text-sm text-gray-500 dark:text-gray-400">All Time</p>
                <p className="text-xl font-bold text-gray-900 dark:text-white mt-1">
                  {formatPrice(stats?.totalRevenue || 0)}
                </p>
              </div>
            </div>
            <div className="h-72">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={dailyRevenueData}>
                  <defs>
                    <linearGradient id="revenueGradient" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#10B981" stopOpacity={0.3} />
                      <stop offset="95%" stopColor="#10B981" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
                  <XAxis dataKey="date" stroke="#9CA3AF" fontSize={12} />
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

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5 }}
        >
          <Card>
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-6">
              Orders by Status
            </h2>
            <div className="h-72">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={ordersByStatusData} layout="vertical">
                  <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
                  <XAxis type="number" stroke="#9CA3AF" fontSize={12} />
                  <YAxis dataKey="status" type="category" stroke="#9CA3AF" fontSize={12} width={80} />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: '#1F2937',
                      border: '1px solid #374151',
                      borderRadius: '8px',
                      color: '#F9FAFB',
                    }}
                  />
                  <Bar dataKey="count" fill="#10B981" radius={[0, 4, 4, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </Card>
        </motion.div>
      </div>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.6 }}
      >
        <Card>
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
            Pending Seller Approvals
          </h2>
          <div className="text-center py-8">
            <div className="w-16 h-16 bg-amber-100 dark:bg-amber-900/30 rounded-full flex items-center justify-center mx-auto mb-4">
              <Users className="w-8 h-8 text-amber-600" />
            </div>
            <p className="text-2xl font-bold text-gray-900 dark:text-white">
              {stats?.pendingSellers || 0}
            </p>
            <p className="text-gray-500 dark:text-gray-400">
              sellers waiting for approval
            </p>
          </div>
        </Card>
      </motion.div>
    </div>
  );
}
