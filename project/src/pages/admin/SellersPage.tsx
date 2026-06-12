import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { Search, ChevronLeft, ChevronRight, Eye, Check, X, Ban, RotateCcw } from 'lucide-react';
import api from '../../lib/api';
import type { PagedResponse, User as Seller, ApiResponse, SellerStatus } from '../../types';
import { Card, Button, Badge, Modal, Input } from '../../components/ui';
import { formatDate } from '../../lib/utils';
import { toast } from 'sonner';

export default function SellersPage() {
  const [sellers, setSellers] = useState<PagedResponse<Seller> | null>(null);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<SellerStatus | ''>('');
  const [selectedSeller, setSelectedSeller] = useState<Seller | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [rejectionReason, setRejectionReason] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  useEffect(() => {
    fetchSellers();
  }, []);

  const fetchSellers = async (page = 0) => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      params.append('page', page.toString());
      params.append('size', '10');
      if (statusFilter) params.append('status', statusFilter);

      const response = await api.get<ApiResponse<PagedResponse<Seller>>>(
        `/api/admin/sellers?${params.toString()}`
      );
      setSellers(response.data.data);
    } catch (error) {
      console.error('Failed to fetch sellers:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (sellerId: number) => {
    setActionLoading(true);
    try {
      await api.put(`/api/admin/sellers/${sellerId}/approve`);
      toast.success('Seller approved successfully');
      fetchSellers(sellers?.number || 0);
    } catch (error) {
      toast.error('Failed to approve seller');
    } finally {
      setActionLoading(false);
    }
  };

  const handleReject = async () => {
    if (!selectedSeller) return;
    setActionLoading(true);
    try {
      await api.put(`/api/admin/sellers/${selectedSeller.id}/reject`, { reason: rejectionReason });
      toast.success('Seller rejected');
      setIsModalOpen(false);
      setSelectedSeller(null);
      setRejectionReason('');
      fetchSellers(sellers?.number || 0);
    } catch (error) {
      toast.error('Failed to reject seller');
    } finally {
      setActionLoading(false);
    }
  };

  const handleToggleStatus = async (sellerId: number, enable: boolean) => {
    setActionLoading(true);
    try {
      await api.put(`/api/admin/sellers/${sellerId}/status?enable=${enable}`);
      toast.success(`Seller ${enable ? 'enabled' : 'disabled'}`);
      fetchSellers(sellers?.number || 0);
    } catch (error) {
      toast.error('Failed to update seller status');
    } finally {
      setActionLoading(false);
    }
  };

  const getStatusBadge = (status: SellerStatus) => {
    const variants: Record<SellerStatus, 'success' | 'warning' | 'error' | 'default'> = {
      APPROVED: 'success',
      PENDING: 'warning',
      REJECTED: 'error',
      SUSPENDED: 'error',
    };
    return <Badge variant={variants[status] || 'default'}>{status}</Badge>;
  };

  const filteredSellers = sellers?.content.filter((seller) =>
    seller.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    seller.email.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Seller Management</h1>
          <p className="text-gray-500 dark:text-gray-400">
            Manage and approve seller accounts
          </p>
        </div>
      </div>

      <Card>
        <div className="flex flex-col sm:flex-row gap-4 mb-6">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search sellers..."
              className="w-full pl-10 pr-4 py-2.5 rounded-lg border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
          </div>
          <select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value as SellerStatus | '');
              fetchSellers();
            }}
            className="px-4 py-2.5 rounded-lg border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
          >
            <option value="">All Status</option>
            <option value="PENDING">Pending</option>
            <option value="APPROVED">Approved</option>
            <option value="REJECTED">Rejected</option>
            <option value="SUSPENDED">Suspended</option>
          </select>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-gray-200 dark:border-gray-700">
                <th className="text-left py-4 px-4 text-sm font-semibold text-gray-700 dark:text-gray-300">
                  Seller
                </th>
                <th className="text-left py-4 px-4 text-sm font-semibold text-gray-700 dark:text-gray-300">
                  Shop
                </th>
                <th className="text-left py-4 px-4 text-sm font-semibold text-gray-700 dark:text-gray-300">
                  Status
                </th>
                <th className="text-left py-4 px-4 text-sm font-semibold text-gray-700 dark:text-gray-300">
                  Joined
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
                    <td className="py-4 px-4">
                      <div className="animate-pulse h-4 w-32 bg-gray-200 dark:bg-gray-700 rounded" />
                    </td>
                    <td className="py-4 px-4">
                      <div className="animate-pulse h-4 w-24 bg-gray-200 dark:bg-gray-700 rounded" />
                    </td>
                    <td className="py-4 px-4">
                      <div className="animate-pulse h-6 w-20 bg-gray-200 dark:bg-gray-700 rounded" />
                    </td>
                    <td className="py-4 px-4">
                      <div className="animate-pulse h-4 w-24 bg-gray-200 dark:bg-gray-700 rounded" />
                    </td>
                    <td className="py-4 px-4">
                      <div className="animate-pulse h-8 w-24 bg-gray-200 dark:bg-gray-700 rounded ml-auto" />
                    </td>
                  </tr>
                ))
              ) : filteredSellers?.length === 0 ? (
                <tr>
                  <td colSpan={5} className="text-center py-12 text-gray-500 dark:text-gray-400">
                    No sellers found
                  </td>
                </tr>
              ) : (
                filteredSellers?.map((seller) => (
                  <motion.tr
                    key={seller.id}
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    className="border-b border-gray-100 dark:border-gray-800 hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors"
                  >
                    <td className="py-4 px-4">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-full bg-gradient-to-br from-emerald-500 to-teal-600 flex items-center justify-center text-white font-medium">
                          {seller.name.charAt(0)}
                        </div>
                        <div>
                          <p className="font-medium text-gray-900 dark:text-white">{seller.name}</p>
                          <p className="text-sm text-gray-500 dark:text-gray-400">{seller.email}</p>
                        </div>
                      </div>
                    </td>
                    <td className="py-4 px-4">
                      <p className="text-gray-900 dark:text-white">{(seller as Seller & { shopName?: string }).shopName || 'N/A'}</p>
                    </td>
                    <td className="py-4 px-4">
                      {getStatusBadge((seller as Seller & { sellerStatus: SellerStatus }).sellerStatus || 'PENDING')}
                    </td>
                    <td className="py-4 px-4 text-gray-500 dark:text-gray-400">
                      {formatDate(seller.createdAt)}
                    </td>
                    <td className="py-4 px-4">
                      <div className="flex items-center justify-end gap-2">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => window.open(`/admin/sellers/${seller.id}`, '_blank')}
                        >
                          <Eye className="w-4 h-4" />
                        </Button>
                        {(seller as Seller & { sellerStatus: SellerStatus }).sellerStatus === 'PENDING' && (
                          <>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleApprove(seller.id)}
                              disabled={actionLoading}
                              className="text-emerald-600 hover:bg-emerald-50"
                            >
                              <Check className="w-4 h-4" />
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => {
                                setSelectedSeller(seller);
                                setIsModalOpen(true);
                              }}
                              disabled={actionLoading}
                              className="text-red-600 hover:bg-red-50"
                            >
                              <X className="w-4 h-4" />
                            </Button>
                          </>
                        )}
                        {(seller as Seller & { sellerStatus: SellerStatus }).sellerStatus === 'APPROVED' && (
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleToggleStatus(seller.id, false)}
                            disabled={actionLoading}
                            className="text-red-600 hover:bg-red-50"
                          >
                            <Ban className="w-4 h-4" />
                          </Button>
                        )}
                        {(seller as Seller & { sellerStatus: SellerStatus }).sellerStatus === 'SUSPENDED' && (
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleToggleStatus(seller.id, true)}
                            disabled={actionLoading}
                            className="text-emerald-600 hover:bg-emerald-50"
                          >
                            <RotateCcw className="w-4 h-4" />
                          </Button>
                        )}
                      </div>
                    </td>
                  </motion.tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {sellers && sellers.totalPages > 1 && (
          <div className="flex items-center justify-between mt-6 pt-6 border-t border-gray-200 dark:border-gray-700">
            <p className="text-sm text-gray-500 dark:text-gray-400">
              Showing {sellers.number * sellers.size + 1} to{' '}
              {Math.min((sellers.number + 1) * sellers.size, sellers.totalElements)} of{' '}
              {sellers.totalElements} sellers
            </p>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => fetchSellers(sellers.number - 1)}
                disabled={sellers.first}
              >
                <ChevronLeft className="w-4 h-4" />
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => fetchSellers(sellers.number + 1)}
                disabled={sellers.last}
              >
                <ChevronRight className="w-4 h-4" />
              </Button>
            </div>
          </div>
        )}
      </Card>

      <Modal
        isOpen={isModalOpen}
        onClose={() => {
          setIsModalOpen(false);
          setSelectedSeller(null);
          setRejectionReason('');
        }}
        title="Reject Seller"
      >
        <div className="space-y-4">
          <p className="text-gray-600 dark:text-gray-400">
            Are you sure you want to reject {selectedSeller?.name}? Please provide a reason.
          </p>
          <Input
            label="Rejection Reason"
            value={rejectionReason}
            onChange={(e) => setRejectionReason(e.target.value)}
            placeholder="Enter reason for rejection..."
          />
          <div className="flex justify-end gap-3">
            <Button
              variant="outline"
              onClick={() => {
                setIsModalOpen(false);
                setSelectedSeller(null);
                setRejectionReason('');
              }}
            >
              Cancel
            </Button>
            <Button
              variant="danger"
              onClick={handleReject}
              isLoading={actionLoading}
            >
              Reject Seller
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
