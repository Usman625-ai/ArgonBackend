import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import {
  Package,
  ShoppingCart,
  DollarSign,
  AlertTriangle,
  TrendingUp,
  Calendar,
} from 'lucide-react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import api from '../../lib/api';
import type { SellerDashboardStats, ApiResponse } from '../../types';
import { Card, CardSkeleton } from '../../components/ui';
import { formatPrice } from '../../lib/utils';
import { useAppSelector } from '../../store';

const weeklySalesData = [
  { date: 'Mon', amount: 12000 },
  { date: 'Tue', amount: 15000 },
  { date: 'Wed', amount: 8500 },
  { date: 'Thu', amount: 18000 },
  { date: 'Fri', amount: 22000 },
  { date: 'Sat', amount: 25000 },
  { date: 'Sun', amount: 19000 },
];

export default function SellerDashboard() {
  const [stats, setStats] = useState<SellerDashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const { user } = useAppSelector((state) => state.auth);

  useEffect(() => {
    fetchStats();
  }, []);

  const fetchStats = async () => {
    try {
      const response = await api.get<ApiResponse<SellerDashboardStats>>('/api/seller/dashboard/stats');
      setStats(response.data.data);
    } catch (error) {
      console.error('Failed to fetch stats:', error);
    } finally {
      setLoading(false);
    }
  };

  const statCards = [
    {
      icon: Package,
      label: 'Total Products',
      value: stats?.totalProducts || 0,
      color: 'from-blue-500 to-indigo-600',
    },
    {
      icon: ShoppingCart,
      label: 'Total Orders',
      value: stats?.totalOrders || 0,
      color: 'from-purple-500 to-pink-600',
    },
    {
      icon: DollarSign,
      label: 'Total Revenue',
      value: formatPrice(stats?.totalRevenue || 0),
      color: 'from-emerald-500 to-teal-600',
    },
    {
      icon: AlertTriangle,
      label: 'Low Stock Items',
      value: stats?.lowStockCount || 0,
      color: 'from-orange-500 to-red-600',
    },
  ];

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
            Welcome, {user?.name || 'Seller'}!
          </h1>
          <p className="text-gray-500 dark:text-gray-400">
            Here's your shop performance overview
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
          {statCards.map((card, i) => (
            <motion.div key={i} variants={item}>
              <Card className="relative overflow-hidden">
                <div className={`absolute top-0 right-0 w-32 h-32 bg-gradient-to-br ${card.color} opacity-10 rounded-full -translate-y-1/2 translate-x-1/2`} />
                <div className="relative">
                  <div className={`inline-flex p-3 rounded-xl bg-gradient-to-br ${card.color} mb-4`}>
                    <card.icon className="w-6 h-6 text-white" />
                  </div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">{card.label}</p>
                  <p className="text-3xl font-bold text-gray-900 dark:text-white mt-1">
                    {card.value}
                  </p>
                </div>
              </Card>
            </motion.div>
          ))}
        </motion.div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
        >
          <Card>
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
                Weekly Sales
              </h2>
              <div className="flex items-center gap-2">
                <TrendingUp className="w-5 h-5 text-emerald-500" />
                <span className="text-emerald-600 text-sm font-medium">+18.2%</span>
              </div>
            </div>
            <div className="h-72">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={stats?.weeklySales || weeklySalesData}>
                  <defs>
                    <linearGradient id="salesGradient" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#8B5CF6" stopOpacity={0.3} />
                      <stop offset="95%" stopColor="#8B5CF6" stopOpacity={0} />
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
                  <Line
                    type="monotone"
                    dataKey="amount"
                    stroke="#8B5CF6"
                    strokeWidth={3}
                    dot={{ fill: '#8B5CF6', strokeWidth: 2 }}
                    fill="url(#salesGradient)"
                  />
                </LineChart>
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
              Recent Activity
            </h2>
            <div className="space-y-4">
              <div className="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-800 rounded-lg">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-emerald-100 dark:bg-emerald-900/30 rounded-full flex items-center justify-center">
                    <ShoppingCart className="w-5 h-5 text-emerald-600" />
                  </div>
                  <div>
                    <p className="font-medium text-gray-900 dark:text-white">New Order</p>
                    <p className="text-sm text-gray-500 dark:text-gray-400">ORD-2024-0089</p>
                  </div>
                </div>
                <span className="text-emerald-600 font-medium">+PKR 4,500</span>
              </div>
              <div className="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-800 rounded-lg">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-blue-100 dark:bg-blue-900/30 rounded-full flex items-center justify-center">
                    <Package className="w-5 h-5 text-blue-600" />
                  </div>
                  <div>
                    <p className="font-medium text-gray-900 dark:text-white">Order Shipped</p>
                    <p className="text-sm text-gray-500 dark:text-gray-400">ORD-2024-0085</p>
                  </div>
                </div>
                <span className="text-blue-600 font-medium">In Transit</span>
              </div>
              <div className="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-800 rounded-lg">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-orange-100 dark:bg-orange-900/30 rounded-full flex items-center justify-center">
                    <AlertTriangle className="w-5 h-5 text-orange-600" />
                  </div>
                  <div>
                    <p className="font-medium text-gray-900 dark:text-white">Low Stock Alert</p>
                    <p className="text-sm text-gray-500 dark:text-gray-400">Product "XYZ Item"</p>
                  </div>
                </div>
                <span className="text-orange-600 font-medium">3 left</span>
              </div>
            </div>
          </Card>
        </motion.div>
      </div>

      {stats?.pendingOrders && stats.pendingOrders > 0 && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.6 }}
        >
          <Card className="bg-gradient-to-r from-amber-50 to-orange-50 dark:from-amber-900/20 dark:to-orange-900/20 border-amber-200 dark:border-amber-800">
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 bg-amber-100 dark:bg-amber-900/30 rounded-xl flex items-center justify-center">
                <ShoppingCart className="w-6 h-6 text-amber-600" />
              </div>
              <div className="flex-1">
                <p className="font-semibold text-gray-900 dark:text-white">
                  You have {stats.pendingOrders} pending order(s)
                </p>
                <p className="text-sm text-gray-600 dark:text-gray-400">
                  Process pending orders to maintain good seller rating
                </p>
              </div>
              <a
                href="/seller/orders?status=PENDING"
                className="px-4 py-2 bg-amber-600 text-white rounded-lg hover:bg-amber-700 transition-colors"
              >
                View Orders
              </a>
            </div>
          </Card>
        </motion.div>
      )}
    </div>
  );
}
