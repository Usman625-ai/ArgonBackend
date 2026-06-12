import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { Search, Eye, ChevronLeft, ChevronRight, Package, Truck } from 'lucide-react';
import api from '../../lib/api';
import type { PagedResponse, Order, ApiResponse } from '../../types';
import { Card, Button, OrderStatusBadge, Modal, Input } from '../../components/ui';
import { formatPrice, formatDate } from '../../lib/utils';
import { toast } from 'sonner';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

const statusUpdateSchema = z.object({
  status: z.enum(['PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED']),
  trackingNumber: z.string().optional(),
  comment: z.string().optional(),
});

type StatusUpdateFormData = z.infer<typeof statusUpdateSchema>;

export default function SellerOrdersPage() {
  const [orders, setOrders] = useState<PagedResponse<Order> | null>(null);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [updating, setUpdating] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<StatusUpdateFormData>({
    resolver: zodResolver(statusUpdateSchema),
  });

  useEffect(() => {
    fetchOrders();
  }, []);

  const fetchOrders = async (page = 0) => {
    setLoading(true);
    try {
      const response = await api.get<ApiResponse<PagedResponse<Order>>>(
        `/api/seller/orders?page=${page}&size=10`
      );
      setOrders(response.data.data);
    } catch (error) {
      console.error('Failed to fetch orders:', error);
    } finally {
      setLoading(false);
    }
  };

  const openOrderDetails = async (orderId: number) => {
    try {
      const response = await api.get<ApiResponse<Order>>(`/api/seller/orders/${orderId}`);
      setSelectedOrder(response.data.data);
      reset({
        status: response.data.data.status,
        trackingNumber: response.data.data.trackingNumber || '',
        comment: '',
      });
      setIsModalOpen(true);
    } catch (error) {
      console.error('Failed to fetch order details:', error);
    }
  };

  const handleUpdateStatus = async (data: StatusUpdateFormData) => {
    if (!selectedOrder) return;
    setUpdating(true);
    try {
      await api.put(`/api/seller/orders/${selectedOrder.id}/status`, data);
      toast.success('Order status updated');
      setIsModalOpen(false);
      fetchOrders(orders?.number || 0);
    } catch (error) {
      toast.error('Failed to update order status');
    } finally {
      setUpdating(false);
    }
  };

  const filteredOrders = orders?.content.filter(
    (order) =>
      order.orderNumber.toLowerCase().includes(searchQuery.toLowerCase()) ||
      order.customer?.name?.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Orders</h1>
        <p className="text-gray-500 dark:text-gray-400">
          Manage and process your orders
        </p>
      </div>

      <Card>
        <div className="mb-6">
          <div className="relative max-w-md">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search orders..."
              className="w-full pl-10 pr-4 py-2.5 rounded-lg border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-gray-200 dark:border-gray-700">
                <th className="text-left py-4 px-4 text-sm font-semibold text-gray-700 dark:text-gray-300">
                  Order
                </th>
                <th className="text-left py-4 px-4 text-sm font-semibold text-gray-700 dark:text-gray-300">
                  Customer
                </th>
                <th className="text-left py-4 px-4 text-sm font-semibold text-gray-700 dark:text-gray-300">
                  Items
                </th>
                <th className="text-left py-4 px-4 text-sm font-semibold text-gray-700 dark:text-gray-300">
                  Total
                </th>
                <th className="text-left py-4 px-4 text-sm font-semibold text-gray-700 dark:text-gray-300">
                  Status
                </th>
                <th className="text-left py-4 px-4 text-sm font-semibold text-gray-700 dark:text-gray-300">
                  Date
                </th>
                <th className="text-right py-4 px-4 text-sm font-semibold text-gray-700 dark:text-gray-300">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                [...Array(5)].map((_, i) => (
                  <tr key={i} className="border-b border-gray-100 dark:border-gray-800">
                    {[...Array(7)].map((_, j) => (
                      <td key={j} className="py-4 px-4">
                        <div className="animate-pulse h-4 w-20 bg-gray-200 dark:bg-gray-700 rounded" />
                      </td>
                    ))}
                  </tr>
                ))
              ) : filteredOrders?.length === 0 ? (
                <tr>
                  <td colSpan={7} className="text-center py-12 text-gray-500 dark:text-gray-400">
                    No orders found
                  </td>
                </tr>
              ) : (
                filteredOrders?.map((order) => (
                  <motion.tr
                    key={order.id}
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    className="border-b border-gray-100 dark:border-gray-800 hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors"
                  >
                    <td className="py-4 px-4">
                      <p className="font-medium text-gray-900 dark:text-white">
                        {order.orderNumber}
                      </p>
                    </td>
                    <td className="py-4 px-4">
                      <p className="text-gray-900 dark:text-white">{order.customer?.name}</p>
                    </td>
                    <td className="py-4 px-4">
                      <p className="text-gray-900 dark:text-white">{order.items?.length || 0} items</p>
                    </td>
                    <td className="py-4 px-4">
                      <p className="font-medium text-gray-900 dark:text-white">
                        {formatPrice(order.total)}
                      </p>
                    </td>
                    <td className="py-4 px-4">
                      <OrderStatusBadge status={order.status} />
                    </td>
                    <td className="py-4 px-4 text-gray-500 dark:text-gray-400">
                      {formatDate(order.createdAt)}
                    </td>
                    <td className="py-4 px-4">
                      <div className="flex justify-end">
                        <Button variant="ghost" size="sm" onClick={() => openOrderDetails(order.id)}>
                          <Eye className="w-4 h-4" />
                        </Button>
                      </div>
                    </td>
                  </motion.tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {orders && orders.totalPages > 1 && (
          <div className="flex items-center justify-between mt-6 pt-6 border-t border-gray-200 dark:border-gray-700">
            <p className="text-sm text-gray-500 dark:text-gray-400">
              Showing {orders.number * orders.size + 1} to{' '}
              {Math.min((orders.number + 1) * orders.size, orders.totalElements)} of{' '}
              {orders.totalElements} orders
            </p>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => fetchOrders(orders.number - 1)}
                disabled={orders.first}
              >
                <ChevronLeft className="w-4 h-4" />
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => fetchOrders(orders.number + 1)}
                disabled={orders.last}
              >
                <ChevronRight className="w-4 h-4" />
              </Button>
            </div>
          </div>
        )}
      </Card>

      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={`Order ${selectedOrder?.orderNumber}`}
        size="full"
      >
        {selectedOrder && (
          <div className="space-y-6">
            <div className="grid grid-cols-2 gap-6">
              <div>
                <h4 className="font-semibold text-gray-900 dark:text-white mb-2">
                  Customer Info
                </h4>
                <p className="text-gray-600 dark:text-gray-400">
                  {selectedOrder.customer?.name}
                  <br />
                  {selectedOrder.customer?.email}
                </p>
              </div>
              <div>
                <h4 className="font-semibold text-gray-900 dark:text-white mb-2">
                  Shipping Address
                </h4>
                <p className="text-gray-600 dark:text-gray-400">
                  {selectedOrder.shippingAddress?.street}
                  <br />
                  {selectedOrder.shippingAddress?.city}, {selectedOrder.shippingAddress?.state}
                </p>
              </div>
            </div>

            <div>
              <h4 className="font-semibold text-gray-900 dark:text-white mb-3">Items</h4>
              <div className="space-y-3">
                {selectedOrder.items?.map((item) => (
                  <div
                    key={item.id}
                    className="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-800 rounded-lg"
                  >
                    <div className="flex items-center gap-3">
                      <Package className="w-8 h-8 text-gray-400" />
                      <div>
                        <p className="font-medium text-gray-900 dark:text-white">
                          {item.product?.name}
                        </p>
                        <p className="text-sm text-gray-500 dark:text-gray-400">
                          Qty: {item.quantity} x {formatPrice(item.price)}
                        </p>
                      </div>
                    </div>
                    <p className="font-medium text-gray-900 dark:text-white">
                      {formatPrice(item.total)}
                    </p>
                  </div>
                ))}
              </div>
            </div>

            <form onSubmit={handleSubmit(handleUpdateStatus)} className="space-y-4 border-t border-gray-200 dark:border-gray-700 pt-6">
              <h4 className="font-semibold text-gray-900 dark:text-white">Update Status</h4>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Status
                  </label>
                  <select
                    {...register('status')}
                    className="w-full px-4 py-2.5 rounded-lg border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  >
                    <option value="PENDING">Pending</option>
                    <option value="PROCESSING">Processing</option>
                    <option value="SHIPPED">Shipped</option>
                    <option value="DELIVERED">Delivered</option>
                    <option value="CANCELLED">Cancelled</option>
                  </select>
                </div>
                <Input
                  {...register('trackingNumber')}
                  label="Tracking Number"
                  placeholder="Enter tracking number"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Comment
                </label>
                <textarea
                  {...register('comment')}
                  rows={2}
                  placeholder="Optional note..."
                  className="w-full px-4 py-2.5 rounded-lg border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>
              <div className="flex justify-end gap-3">
                <Button type="button" variant="outline" onClick={() => setIsModalOpen(false)}>
                  Cancel
                </Button>
                <Button type="submit" isLoading={updating}>
                  <Truck className="w-4 h-4 mr-2" />
                  Update Status
                </Button>
              </div>
            </form>
          </div>
        )}
      </Modal>
    </div>
  );
}
