import Badge from './Badge';
import type { OrderStatus, SellerStatus, PaymentStatus } from '../../types';

interface StatusBadgeProps {
  status: OrderStatus | SellerStatus | PaymentStatus | string;
}

const orderStatusVariant: Record<string, 'default' | 'success' | 'warning' | 'error' | 'info'> = {
  PENDING: 'warning',
  PROCESSING: 'info',
  SHIPPED: 'info',
  DELIVERED: 'success',
  CANCELLED: 'error',
};

const sellerStatusVariant: Record<string, 'default' | 'success' | 'warning' | 'error' | 'info'> = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'error',
  SUSPENDED: 'error',
};

const paymentStatusVariant: Record<string, 'default' | 'success' | 'warning' | 'error' | 'info'> = {
  PENDING: 'warning',
  PAID: 'success',
  FAILED: 'error',
  REFUNDED: 'info',
};

export function OrderStatusBadge({ status }: StatusBadgeProps) {
  return <Badge variant={orderStatusVariant[status] || 'default'}>{status}</Badge>;
}

export function SellerStatusBadge({ status }: StatusBadgeProps) {
  return <Badge variant={sellerStatusVariant[status] || 'default'}>{status}</Badge>;
}

export function PaymentStatusBadge({ status }: StatusBadgeProps) {
  return <Badge variant={paymentStatusVariant[status] || 'default'}>{status}</Badge>;
}
