import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Package, ChevronRight, Eye } from 'lucide-react';
import api from '../../lib/api';
import type { Order, PagedResponse, ApiResponse } from '../../types';
import { formatPrice, formatDate } from '../../lib/utils';
import { Card, OrderStatusBadge, Badge, Modal, Button } from '../../components/ui';

export default function OrdersPage() {
  const [orders, setOrders] = useState<PagedResponse<Order> | null>(null);
  const [loading, setLoading] = useState(true);
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);

  useEffect(() => {
    fetchOrders();
  }, []);

  const fetchOrders = async () => {
    setLoading(true);
    try {
      const response = await api.get<ApiResponse<PagedResponse<Order>>>(
        '/api/customer/orders?page=0&size=20'
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
      const response = await api.get<ApiResponse<Order>>(`/api/customer/orders/${orderId}`);
      setSelectedOrder(response.data.data);
      setIsModalOpen(true);
    } catch (error) {
      console.error('Failed to fetch order details:', error);
    }
  };

  const handleCancelOrder = async (orderId: number) => {
    if (!confirm('Are you sure you want to cancel this order?')) return;
    try {
      await api.put(`/api/customer/orders/${orderId}/cancel`);
      fetchOrders();
    } catch (error) {
      console.error('Failed to cancel order:', error);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950 p-8">
        <div className="max-w-4xl mx-auto space-y-4">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="animate-pulse h-32 bg-gray-200 dark:bg-gray-800 rounded-xl" />
          ))}
        </div>
      </div>
    );
  }

  if (!orders || orders.content.length === 0) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950 flex items-center justify-center p-8">
        <div className="text-center">
          <Package className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">
            No orders yet
          </h2>
          <p className="text-gray-500 dark:text-gray-400 mb-6">
            Start shopping to see your orders here
          </p>
          <Link to="/shop/products">
            <Button>
              Start Shopping
            </Button>
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950 py-8">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-8">
          My Orders
        </h1>

        <div className="space-y-4">
          {orders.content.map((order, index) => (
            <motion.div
              key={order.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.05 }}
            >
              <Card className="hover:shadow-lg transition-shadow">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                  <div className="flex items-center gap-4">
                    <div className="w-16 h-16 bg-gray-100 dark:bg-gray-700 rounded-xl flex items-center justify-center">
                      <Package className="w-8 h-8 text-gray-400" />
                    </div>
                    <div>
                      <p className="font-semibold text-gray-900 dark:text-white">
                        {order.orderNumber}
                      </p>
                      <p className="text-sm text-gray-500 dark:text-gray-400">
                        {formatDate(order.createdAt)} - {order.items.length} items
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center gap-4">
                    <div className="text-right">
                      <p className="font-semibold text-gray-900 dark:text-white">
                        {formatPrice(order.total)}
                      </p>
                      <OrderStatusBadge status={order.status} />
                    </div>
                    <div className="flex gap-2">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => openOrderDetails(order.id)}
                      >
                        <Eye className="w-4 h-4" />
                      </Button>
                      {(order.status === 'PENDING' || order.status === 'PROCESSING') && (
                        <Button
                          variant="ghost"
                          size="sm"
                          className="text-red-600"
                          onClick={() => handleCancelOrder(order.id)}
                        >
                          Cancel
                        </Button>
                      )}
                    </div>
                  </div>
                </div>
              </Card>
            </motion.div>
          ))}
        </div>
      </div>

      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={`Order ${selectedOrder?.orderNumber}`}
        size="full"
      >
        {selectedOrder && (
          <div className="space-y-6">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-xs text-gray-500 dark:text-gray-400 uppercase">Status</p>
                <OrderStatusBadge status={selectedOrder.status} />
              </div>
              <div>
                <p className="text-xs text-gray-500 dark:text-gray-400 uppercase">Payment</p>
                <Badge variant={selectedOrder.paymentStatus === 'PAID' ? 'success' : 'warning'}>
                  {selectedOrder.paymentStatus}
                </Badge>
              </div>
            </div>

            <div>
              <h4 className="font-semibold text-gray-900 dark:text-white mb-3">Items</h4>
              <div className="space-y-3">
                {selectedOrder.items.map((item) => (
                  <div
                    key={item.id}
                    className="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-800 rounded-lg"
                  >
                    <div className="flex items-center gap-3">
                      <div className="w-12 h-12 bg-gray-100 dark:bg-gray-700 rounded-lg overflow-hidden">
                        {item.product.images[0] && (
                          <img
                            src={item.product.images[0]}
                            alt={item.product.name}
                            className="w-full h-full object-cover"
                          />
                        )}
                      </div>
                      <div>
                        <p className="font-medium text-gray-900 dark:text-white">
                          {item.product.name}
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

            <div className="border-t border-gray-200 dark:border-gray-700 pt-4 space-y-2">
              <div className="flex justify-between">
                <span className="text-gray-600 dark:text-gray-400">Subtotal</span>
                <span className="text-gray-900 dark:text-white">{formatPrice(selectedOrder.subtotal)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600 dark:text-gray-400">Shipping</span>
                <span className="text-gray-900 dark:text-white">{formatPrice(selectedOrder.shipping)}</span>
              </div>
              {selectedOrder.discount > 0 && (
                <div className="flex justify-between text-emerald-600">
                  <span>Discount</span>
                  <span>-{formatPrice(selectedOrder.discount)}</span>
                </div>
              )}
              <div className="flex justify-between font-semibold text-lg">
                <span className="text-gray-900 dark:text-white">Total</span>
                <span className="text-gray-900 dark:text-white">{formatPrice(selectedOrder.total)}</span>
              </div>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
