import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { Plus, Edit, Trash2, Tag, ChevronLeft, ChevronRight, Percent, DollarSign } from 'lucide-react';
import api from '../../lib/api';
import type { PagedResponse, Coupon, ApiResponse } from '../../types';
import { Card, Button, Badge, Modal, Input } from '../../components/ui';
import { formatDate } from '../../lib/utils';
import { toast } from 'sonner';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

const couponSchema = z.object({
  code: z.string().min(3, 'Code must be at least 3 characters'),
  type: z.enum(['PERCENTAGE', 'FIXED']),
  value: z.number().min(1, 'Value must be positive'),
  minOrderValue: z.number().min(0),
  maxUses: z.number().min(1),
  validFrom: z.string(),
  validTo: z.string(),
});

type CouponFormData = z.infer<typeof couponSchema>;

export default function CouponsPage() {
  const [coupons, setCoupons] = useState<PagedResponse<Coupon> | null>(null);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingCoupon, setEditingCoupon] = useState<Coupon | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CouponFormData>({
    resolver: zodResolver(couponSchema),
  });

  useEffect(() => {
    fetchCoupons();
  }, []);

  const fetchCoupons = async (page = 0) => {
    setLoading(true);
    try {
      const response = await api.get<ApiResponse<PagedResponse<Coupon>>>(
        `/api/admin/coupons?page=${page}&size=10`
      );
      setCoupons(response.data.data);
    } catch (error) {
      console.error('Failed to fetch coupons:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = () => {
    setEditingCoupon(null);
    reset({
      code: '',
      type: 'PERCENTAGE',
      value: 10,
      minOrderValue: 0,
      maxUses: 100,
      validFrom: new Date().toISOString().split('T')[0],
      validTo: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
    });
    setIsModalOpen(true);
  };

  const handleEdit = (coupon: Coupon) => {
    setEditingCoupon(coupon);
    reset({
      code: coupon.code,
      type: coupon.type,
      value: coupon.value,
      minOrderValue: coupon.minOrderValue,
      maxUses: coupon.maxUses,
      validFrom: new Date(coupon.validFrom).toISOString().split('T')[0],
      validTo: new Date(coupon.validTo).toISOString().split('T')[0],
    });
    setIsModalOpen(true);
  };

  const handleDelete = async (couponId: number) => {
    if (!confirm('Are you sure you want to delete this coupon?')) return;

    try {
      await api.delete(`/api/admin/coupons/${couponId}`);
      toast.success('Coupon deleted');
      fetchCoupons(coupons?.number || 0);
    } catch (error) {
      toast.error('Failed to delete coupon');
    }
  };

  const onSubmit = async (data: CouponFormData) => {
    try {
      const payload = {
        ...data,
        validFrom: new Date(data.validFrom).toISOString(),
        validTo: new Date(data.validTo).toISOString(),
      };

      if (editingCoupon) {
        await api.put(`/api/admin/coupons/${editingCoupon.id}`, payload);
        toast.success('Coupon updated');
      } else {
        await api.post('/api/admin/coupons', payload);
        toast.success('Coupon created');
      }
      setIsModalOpen(false);
      fetchCoupons(coupons?.number || 0);
    } catch (error) {
      toast.error('Failed to save coupon');
    }
  };

  const isExpired = (coupon: Coupon) => new Date(coupon.validTo) < new Date();

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Coupon Management</h1>
          <p className="text-gray-500 dark:text-gray-400">
            Create and manage discount coupons
          </p>
        </div>
        <Button onClick={handleCreate}>
          <Plus className="w-4 h-4 mr-2" />
          Create Coupon
        </Button>
      </div>

      <Card>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-gray-200 dark:border-gray-700">
                <th className="text-left py-4 px-4 text-sm font-semibold text-gray-700 dark:text-gray-300">
                  Code
                </th>
                <th className="text-left py-4 px-4 text-sm font-semibold text-gray-700 dark:text-gray-300">
                  Discount
                </th>
                <th className="text-left py-4 px-4 text-sm font-semibold text-gray-700 dark:text-gray-300">
                  Min Order
                </th>
                <th className="text-left py-4 px-4 text-sm font-semibold text-gray-700 dark:text-gray-300">
                  Usage
                </th>
                <th className="text-left py-4 px-4 text-sm font-semibold text-gray-700 dark:text-gray-300">
                  Validity
                </th>
                <th className="text-left py-4 px-4 text-sm font-semibold text-gray-700 dark:text-gray-300">
                  Status
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
                      <div className="animate-pulse h-4 w-24 bg-gray-200 dark:bg-gray-700 rounded" />
                    </td>
                    <td className="py-4 px-4">
                      <div className="animate-pulse h-4 w-16 bg-gray-200 dark:bg-gray-700 rounded" />
                    </td>
                    <td className="py-4 px-4">
                      <div className="animate-pulse h-4 w-20 bg-gray-200 dark:bg-gray-700 rounded" />
                    </td>
                    <td className="py-4 px-4">
                      <div className="animate-pulse h-4 w-16 bg-gray-200 dark:bg-gray-700 rounded" />
                    </td>
                    <td className="py-4 px-4">
                      <div className="animate-pulse h-4 w-32 bg-gray-200 dark:bg-gray-700 rounded" />
                    </td>
                    <td className="py-4 px-4">
                      <div className="animate-pulse h-6 w-20 bg-gray-200 dark:bg-gray-700 rounded" />
                    </td>
                    <td className="py-4 px-4">
                      <div className="animate-pulse h-8 w-24 bg-gray-200 dark:bg-gray-700 rounded ml-auto" />
                    </td>
                  </tr>
                ))
              ) : coupons?.content.length === 0 ? (
                <tr>
                  <td colSpan={7} className="text-center py-12 text-gray-500 dark:text-gray-400">
                    No coupons found
                  </td>
                </tr>
              ) : (
                coupons?.content.map((coupon) => (
                  <motion.tr
                    key={coupon.id}
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    className="border-b border-gray-100 dark:border-gray-800 hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors"
                  >
                    <td className="py-4 px-4">
                      <div className="flex items-center gap-2">
                        <Tag className="w-4 h-4 text-emerald-500" />
                        <code className="px-2 py-1 bg-gray-100 dark:bg-gray-700 rounded text-sm font-mono">
                          {coupon.code}
                        </code>
                      </div>
                    </td>
                    <td className="py-4 px-4">
                      <div className="flex items-center gap-1">
                        {coupon.type === 'PERCENTAGE' ? (
                          <Percent className="w-4 h-4 text-gray-400" />
                        ) : (
                          <DollarSign className="w-4 h-4 text-gray-400" />
                        )}
                        <span className="text-gray-900 dark:text-white">
                          {coupon.type === 'PERCENTAGE' ? `${coupon.value}%` : `$${coupon.value}`}
                        </span>
                      </div>
                    </td>
                    <td className="py-4 px-4 text-gray-500 dark:text-gray-400">
                      ${coupon.minOrderValue}
                    </td>
                    <td className="py-4 px-4">
                      <span className="text-gray-900 dark:text-white">
                        {coupon.usedCount}
                      </span>
                      <span className="text-gray-500 dark:text-gray-400"> / {coupon.maxUses}</span>
                    </td>
                    <td className="py-4 px-4">
                      <p className="text-sm text-gray-500 dark:text-gray-400">
                        {formatDate(coupon.validFrom)} - {formatDate(coupon.validTo)}
                      </p>
                    </td>
                    <td className="py-4 px-4">
                      <Badge variant={isExpired(coupon) ? 'error' : coupon.active ? 'success' : 'default'}>
                        {isExpired(coupon) ? 'Expired' : coupon.active ? 'Active' : 'Inactive'}
                      </Badge>
                    </td>
                    <td className="py-4 px-4">
                      <div className="flex items-center justify-end gap-2">
                        <Button variant="ghost" size="sm" onClick={() => handleEdit(coupon)}>
                          <Edit className="w-4 h-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleDelete(coupon.id)}
                          className="text-red-600 hover:bg-red-50"
                        >
                          <Trash2 className="w-4 h-4" />
                        </Button>
                      </div>
                    </td>
                  </motion.tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {coupons && coupons.totalPages > 1 && (
          <div className="flex items-center justify-between mt-6 pt-6 border-t border-gray-200 dark:border-gray-700">
            <p className="text-sm text-gray-500 dark:text-gray-400">
              Showing {coupons.number * coupons.size + 1} to{' '}
              {Math.min((coupons.number + 1) * coupons.size, coupons.totalElements)} of{' '}
              {coupons.totalElements} coupons
            </p>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => fetchCoupons(coupons.number - 1)}
                disabled={coupons.first}
              >
                <ChevronLeft className="w-4 h-4" />
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => fetchCoupons(coupons.number + 1)}
                disabled={coupons.last}
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
        title={editingCoupon ? 'Edit Coupon' : 'Create Coupon'}
      >
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <Input
            {...register('code')}
            label="Code"
            placeholder="SUMMER2024"
            error={errors.code?.message}
          />

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Type
              </label>
              <select
                {...register('type')}
                className="w-full px-4 py-2.5 rounded-lg border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
              >
                <option value="PERCENTAGE">Percentage</option>
                <option value="FIXED">Fixed Amount</option>
              </select>
            </div>
            <Input
              {...register('value', { valueAsNumber: true })}
              label="Value"
              type="number"
              placeholder="10"
              error={errors.value?.message}
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <Input
              {...register('minOrderValue', { valueAsNumber: true })}
              label="Min Order Value"
              type="number"
              placeholder="0"
            />
            <Input
              {...register('maxUses', { valueAsNumber: true })}
              label="Max Uses"
              type="number"
              placeholder="100"
              error={errors.maxUses?.message}
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <Input
              {...register('validFrom')}
              label="Valid From"
              type="date"
              error={errors.validFrom?.message}
            />
            <Input
              {...register('validTo')}
              label="Valid To"
              type="date"
              error={errors.validTo?.message}
            />
          </div>

          <div className="flex justify-end gap-3 pt-4">
            <Button type="button" variant="outline" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit">
              {editingCoupon ? 'Update Coupon' : 'Create Coupon'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
