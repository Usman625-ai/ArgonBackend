import { Navigate } from 'react-router-dom';
import { useAppSelector } from '../../store';

export default function LandingPage() {
  const { isAuthenticated, user } = useAppSelector((state) => state.auth);

  if (isAuthenticated && user) {
    if (user.role === 'ADMIN') {
      return <Navigate to="/admin/dashboard" replace />;
    }
    if (user.role === 'SELLER') {
      return <Navigate to="/seller/dashboard" replace />;
    }
    return <Navigate to="/shop" replace />;
  }

  return <Navigate to="/login" replace />;
}
