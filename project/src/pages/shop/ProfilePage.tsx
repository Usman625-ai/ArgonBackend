import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { User, Mail, Phone, MapPin, Plus, Edit, Trash2, Save, Lock } from 'lucide-react';
import { useAppSelector, useAppDispatch } from '../../store';
import api from '../../lib/api';
import type { Address, AddressResponse, ApiResponse } from '../../types';
import { Card, Button, Input, Modal } from '../../components/ui';
import { toast } from 'sonner';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { formatPrice } from '../../lib/utils';

const profileSchema = z.object({
  name: z.string().min(2, 'Name is required'),
  email: z.string().email('Invalid email'),
  contactNumber: z.string().optional(),
});

const addressSchema = z.object({
  label: z.string().min(2, 'Label is required'),
  street: z.string().min(5, 'Street address is required'),
  city: z.string().min(2, 'City is required'),
  state: z.string().min(2, 'State is required'),
  postalCode: z.string().min(4, 'Postal code is required'),
  phone: z.string().min(10, 'Phone number is required'),
  isDefault: z.boolean().optional(),
});

type ProfileFormData = z.infer<typeof profileSchema>;
type AddressFormData = z.infer<typeof addressSchema>;

export default function ProfilePage() {
  const { user } = useAppSelector((state) => state.auth);
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [isAddressModalOpen, setIsAddressModalOpen] = useState(false);
  const [editingAddress, setEditingAddress] = useState<Address | null>(null);

  const {
    register: registerProfile,
    handleSubmit: handleProfileSubmit,
    formState: { errors: profileErrors },
    reset: resetProfile,
  } = useForm<ProfileFormData>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      name: user?.name || '',
      email: user?.email || '',
      contactNumber: user?.contactNumber || '',
    },
  });

  const {
    register: registerAddress,
    handleSubmit: handleAddressSubmit,
    formState: { errors: addressErrors },
    reset: resetAddress,
  } = useForm<AddressFormData>({
    resolver: zodResolver(addressSchema),
  });

  useEffect(() => {
    fetchAddresses();
  }, []);

  const fetchAddresses = async () => {
    setLoading(true);
    try {
      const response = await api.get<ApiResponse<Address[]>>('/api/customer/addresses');
      setAddresses(response.data.data);
    } catch (error) {
      console.error('Failed to fetch addresses:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateProfile = async (data: ProfileFormData) => {
    setSaving(true);
    try {
      await api.put('/api/customer/profile', data);
      toast.success('Profile updated');
    } catch (error) {
      toast.error('Failed to update profile');
    } finally {
      setSaving(false);
    }
  };

  const handleOpenAddressModal = (address?: Address) => {
    setEditingAddress(address || null);
    if (address) {
      resetAddress({
        label: address.label,
        street: address.street,
        city: address.city,
        state: address.state,
        postalCode: address.postalCode,
        phone: address.phone,
        isDefault: address.isDefault,
      });
    } else {
      resetAddress({
        label: '',
        street: '',
        city: '',
        state: '',
        postalCode: '',
        phone: '',
        isDefault: false,
      });
    }
    setIsAddressModalOpen(true);
  };

  const handleSaveAddress = async (data: AddressFormData) => {
    try {
      if (editingAddress) {
        await api.put(`/api/customer/addresses/${editingAddress.id}`, data);
        toast.success('Address updated');
      } else {
        await api.post('/api/customer/addresses', data);
        toast.success('Address added');
      }
      setIsAddressModalOpen(false);
      fetchAddresses();
    } catch (error) {
      toast.error('Failed to save address');
    }
  };

  const handleDeleteAddress = async (addressId: number) => {
    if (!confirm('Delete this address?')) return;
    try {
      await api.delete(`/api/customer/addresses/${addressId}`);
      toast.success('Address deleted');
      fetchAddresses();
    } catch (error) {
      toast.error('Failed to delete address');
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950 py-8">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-8">
          My Profile
        </h1>

        <div className="grid gap-6 lg:grid-cols-3">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="lg:col-span-2"
          >
            <Card>
              <div className="flex items-center gap-4 mb-6">
                <div className="w-20 h-20 bg-gradient-to-br from-emerald-500 to-teal-600 rounded-full flex items-center justify-center">
                  <span className="text-3xl font-bold text-white">
                    {user?.name?.charAt(0) || 'U'}
                  </span>
                </div>
                <div>
                  <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
                    {user?.name}
                  </h2>
                  <p className="text-gray-500 dark:text-gray-400">{user?.email}</p>
                </div>
              </div>

              <form onSubmit={handleProfileSubmit(handleUpdateProfile)} className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <Input
                    {...registerProfile('name')}
                    label="Full Name"
                    error={profileErrors.name?.message}
                  />
                  <Input
                    {...registerProfile('email')}
                    label="Email"
                    type="email"
                    error={profileErrors.email?.message}
                  />
                </div>
                <Input
                  {...registerProfile('contactNumber')}
                  label="Phone Number"
                  placeholder="+92 300 1234567"
                />
                <div className="flex justify-end">
                  <Button type="submit" isLoading={saving}>
                    <Save className="w-4 h-4 mr-2" />
                    Save Changes
                  </Button>
                </div>
              </form>
            </Card>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
          >
            <Card>
              <div className="space-y-4">
                <div className="flex items-center gap-3 p-4 bg-gray-50 dark:bg-gray-800 rounded-lg">
                  <ShoppingBag className="w-8 h-8 text-emerald-600" />
                  <div>
                    <p className="font-semibold text-gray-900 dark:text-white">Orders</p>
                    <p className="text-2xl font-bold text-emerald-600">12</p>
                  </div>
                </div>
                <div className="flex items-center gap-3 p-4 bg-gray-50 dark:bg-gray-800 rounded-lg">
                  <Heart className="w-8 h-8 text-red-500" />
                  <div>
                    <p className="font-semibold text-gray-900 dark:text-white">Wishlist</p>
                    <p className="text-2xl font-bold text-red-500">5</p>
                  </div>
                </div>
              </div>
            </Card>
          </motion.div>
        </div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="mt-8"
        >
          <Card>
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
                Saved Addresses
              </h2>
              <Button onClick={() => handleOpenAddressModal()}>
                <Plus className="w-4 h-4 mr-2" />
                Add New
              </Button>
            </div>

            {loading ? (
              <div className="space-y-4">
                {[...Array(2)].map((_, i) => (
                  <div key={i} className="animate-pulse h-24 bg-gray-200 dark:bg-gray-700 rounded-lg" />
                ))}
              </div>
            ) : addresses.length === 0 ? (
              <div className="text-center py-8">
                <MapPin className="w-12 h-12 text-gray-300 mx-auto mb-3" />
                <p className="text-gray-500 dark:text-gray-400">No addresses saved</p>
              </div>
            ) : (
              <div className="space-y-3">
                {addresses.map((address) => (
                  <div
                    key={address.id}
                    className="p-4 bg-gray-50 dark:bg-gray-800 rounded-lg flex items-start justify-between"
                  >
                    <div className="flex items-start gap-3">
                      <MapPin className="w-5 h-5 text-gray-400 mt-0.5" />
                      <div>
                        <div className="flex items-center gap-2">
                          <p className="font-medium text-gray-900 dark:text-white">
                            {address.label}
                          </p>
                          {address.isDefault && (
                            <span className="text-xs bg-emerald-100 text-emerald-600 px-2 py-0.5 rounded">
                              Default
                            </span>
                          )}
                        </div>
                        <p className="text-sm text-gray-500 dark:text-gray-400">
                          {address.street}, {address.city}, {address.state} {address.postalCode}
                        </p>
                        <p className="text-sm text-gray-500 dark:text-gray-400">
                          Phone: {address.phone}
                        </p>
                      </div>
                    </div>
                    <div className="flex gap-2">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleOpenAddressModal(address)}
                      >
                        <Edit className="w-4 h-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleDeleteAddress(address.id)}
                        className="text-red-600"
                      >
                        <Trash2 className="w-4 h-4" />
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </Card>
        </motion.div>

        <Modal
          isOpen={isAddressModalOpen}
          onClose={() => setIsAddressModalOpen(false)}
          title={editingAddress ? 'Edit Address' : 'Add Address'}
        >
          <form onSubmit={handleAddressSubmit(handleSaveAddress)} className="space-y-4">
            <Input
              {...registerAddress('label')}
              label="Label (e.g., Home, Office)"
              placeholder="Home"
              error={addressErrors.label?.message}
            />
            <Input
              {...registerAddress('street')}
              label="Street Address"
              placeholder="123 Main Street"
              error={addressErrors.street?.message}
            />
            <div className="grid grid-cols-2 gap-4">
              <Input
                {...registerAddress('city')}
                label="City"
                placeholder="Karachi"
                error={addressErrors.city?.message}
              />
              <Input
                {...registerAddress('state')}
                label="State/Province"
                placeholder="Sindh"
                error={addressErrors.state?.message}
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <Input
                {...registerAddress('postalCode')}
                label="Postal Code"
                placeholder="75000"
                error={addressErrors.postalCode?.message}
              />
              <Input
                {...registerAddress('phone')}
                label="Phone"
                placeholder="+92 300 1234567"
                error={addressErrors.phone?.message}
              />
            </div>
            <label className="flex items-center gap-2">
              <input type="checkbox" {...registerAddress('isDefault')} className="rounded" />
              <span className="text-sm text-gray-700 dark:text-gray-300">
                Set as default address
              </span>
            </label>
            <div className="flex justify-end gap-3 pt-4">
              <Button
                type="button"
                variant="outline"
                onClick={() => setIsAddressModalOpen(false)}
              >
                Cancel
              </Button>
              <Button type="submit">
                {editingAddress ? 'Update Address' : 'Add Address'}
              </Button>
            </div>
          </form>
        </Modal>
      </div>
    </div>
  );
}

function ShoppingBag({ className }: { className?: string }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      fill="none"
      viewBox="0 0 24 24"
      stroke="currentColor"
      strokeWidth="2"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z"
      />
    </svg>
  );
}

function Heart({ className }: { className?: string }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      fill="currentColor"
      viewBox="0 0 24 24"
      stroke="currentColor"
      strokeWidth="2"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"
      />
    </svg>
  );
}
