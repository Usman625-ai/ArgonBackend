import { useState } from 'react';
import { motion } from 'framer-motion';
import { Store, Upload, Save, Phone, FileText } from 'lucide-react';
import { Card, Button, Input } from '../../components/ui';
import { toast } from 'sonner';
import api from '../../lib/api';
import { useAppSelector } from '../../store';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

const shopSettingsSchema = z.object({
  shopName: z.string().min(2, 'Shop name is required'),
  description: z.string().optional(),
  contactNumber: z.string().optional(),
  gstNumber: z.string().optional(),
  panNumber: z.string().optional(),
});

type ShopSettingsFormData = z.infer<typeof shopSettingsSchema>;

export default function SellerSettingsPage() {
  const { user } = useAppSelector((state) => state.auth);
  const [saving, setSaving] = useState(false);
  const [logoPreview, setLogoPreview] = useState<string | null>(null);
  const [bannerPreview, setBannerPreview] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ShopSettingsFormData>({
    resolver: zodResolver(shopSettingsSchema),
    defaultValues: {
      shopName: (user as unknown as { shopName?: string })?.shopName || '',
      description: '',
      contactNumber: user?.contactNumber || '',
      gstNumber: '',
      panNumber: '',
    },
  });

  const onSubmit = async (data: ShopSettingsFormData) => {
    setSaving(true);
    try {
      await api.put('/api/seller/profile', {
        shopName: data.shopName,
        shopDescription: data.description,
        contactNumber: data.contactNumber,
        gstNumber: data.gstNumber,
        panNumber: data.panNumber,
      });
      toast.success('Shop settings updated');
    } catch (error) {
      toast.error('Failed to update settings');
    } finally {
      setSaving(false);
    }
  };

  const handleImageUpload = (type: 'logo' | 'banner') => (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => {
        if (type === 'logo') {
          setLogoPreview(reader.result as string);
        } else {
          setBannerPreview(reader.result as string);
        }
      };
      reader.readAsDataURL(file);
    }
  };

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Shop Settings</h1>
        <p className="text-gray-500 dark:text-gray-400">
          Manage your shop profile and business information
        </p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
          <Card>
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-6 flex items-center gap-2">
              <Store className="w-5 h-5" />
              Basic Information
            </h2>
            <div className="space-y-4">
              <Input
                {...register('shopName')}
                label="Shop Name"
                placeholder="Your shop name"
                error={errors.shopName?.message}
              />
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Description
                </label>
                <textarea
                  {...register('description')}
                  rows={4}
                  placeholder="Describe your shop and what you sell..."
                  className="w-full px-4 py-2.5 rounded-lg border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>
              <Input
                {...register('contactNumber')}
                label="Contact Number"
                placeholder="+92 300 1234567"
              />
            </div>
          </Card>
        </motion.div>

        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}>
          <Card>
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-6 flex items-center gap-2">
              <Upload className="w-5 h-5" />
              Shop Images
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  Shop Logo
                </label>
                <div className="relative">
                  <div className="w-32 h-32 border-2 border-dashed border-gray-300 dark:border-gray-600 rounded-xl flex items-center justify-center overflow-hidden bg-gray-50 dark:bg-gray-800">
                    {logoPreview ? (
                      <img src={logoPreview} alt="Logo" className="w-full h-full object-cover" />
                    ) : (
                      <div className="text-center">
                        <Upload className="w-8 h-8 mx-auto text-gray-400" />
                        <p className="text-xs text-gray-500 mt-1">Upload</p>
                      </div>
                    )}
                  </div>
                  <input
                    type="file"
                    accept="image/*"
                    onChange={handleImageUpload('logo')}
                    className="absolute inset-0 opacity-0 cursor-pointer"
                  />
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  Shop Banner
                </label>
                <div className="relative">
                  <div className="w-full h-32 border-2 border-dashed border-gray-300 dark:border-gray-600 rounded-xl flex items-center justify-center overflow-hidden bg-gray-50 dark:bg-gray-800">
                    {bannerPreview ? (
                      <img src={bannerPreview} alt="Banner" className="w-full h-full object-cover" />
                    ) : (
                      <div className="text-center">
                        <Upload className="w-8 h-8 mx-auto text-gray-400" />
                        <p className="text-xs text-gray-500 mt-1">Upload banner image</p>
                      </div>
                    )}
                  </div>
                  <input
                    type="file"
                    accept="image/*"
                    onChange={handleImageUpload('banner')}
                    className="absolute inset-0 opacity-0 cursor-pointer"
                  />
                </div>
              </div>
            </div>
          </Card>
        </motion.div>

        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.2 }}>
          <Card>
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-6 flex items-center gap-2">
              <FileText className="w-5 h-5" />
              Business Information
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <Input
                {...register('gstNumber')}
                label="GST Number"
                placeholder="GST number (optional)"
              />
              <Input
                {...register('panNumber')}
                label="PAN Number"
                placeholder="PAN number (optional)"
              />
            </div>
          </Card>
        </motion.div>

        <div className="flex justify-end">
          <Button type="submit" isLoading={saving} size="lg">
            <Save className="w-4 h-4 mr-2" />
            Save Changes
          </Button>
        </div>
      </form>
    </div>
  );
}
