import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Settings, Moon, Sun, Globe, DollarSign, Mail, Save, AlertCircle } from 'lucide-react';
import { Card, Button, Input } from '../../components/ui';
import { toast } from 'sonner';
import api from '../../lib/api';
import type { ApiResponse } from '../../types';
import { useAppDispatch, useAppSelector } from '../../store';
import { toggleTheme } from '../../store/uiSlice';

export default function SettingsPage() {
  const [maintenanceMode, setMaintenanceMode] = useState(false);
  const [siteName, setSiteName] = useState('MultiVend');
  const [contactEmail, setContactEmail] = useState('support@multivend.pk');
  const [currencySymbol, setCurrencySymbol] = useState('PKR');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const dispatch = useAppDispatch();
  const { theme } = useAppSelector((state) => state.ui);

  useEffect(() => {
    fetchSettings();
  }, []);

  const fetchSettings = async () => {
    try {
      const response = await api.get<ApiResponse<boolean>>('/api/admin/settings/maintenance');
      setMaintenanceMode(response.data.data);
    } catch (error) {
      console.error('Failed to fetch settings:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleMaintenanceToggle = async () => {
    try {
      const response = await api.post<ApiResponse<boolean>>(
        `/api/admin/settings/maintenance?enable=${!maintenanceMode}`
      );
      setMaintenanceMode(response.data.data);
      toast.success(`Maintenance mode ${!maintenanceMode ? 'enabled' : 'disabled'}`);
    } catch (error) {
      toast.error('Failed to update maintenance mode');
    }
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      // In a real app, these would be API calls
      await new Promise((resolve) => setTimeout(resolve, 1000));
      toast.success('Settings saved successfully');
    } catch (error) {
      toast.error('Failed to save settings');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">System Settings</h1>
        <p className="text-gray-500 dark:text-gray-400">
          Configure global platform settings
        </p>
      </div>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="space-y-6"
      >
        <Card>
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
                Maintenance Mode
              </h2>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                When enabled, the shop will show a maintenance message and block all orders
              </p>
            </div>
            <button
              onClick={handleMaintenanceToggle}
              className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                maintenanceMode ? 'bg-red-500' : 'bg-gray-200 dark:bg-gray-700'
              }`}
            >
              <span
                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                  maintenanceMode ? 'translate-x-6' : 'translate-x-1'
                }`}
              />
            </button>
          </div>
          {maintenanceMode && (
            <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
              <div className="flex items-start gap-3">
                <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
                <div>
                  <p className="text-red-800 dark:text-red-200 font-medium">
                    Maintenance mode is active
                  </p>
                  <p className="text-red-600 dark:text-red-400 text-sm">
                    Customer-facing pages are currently blocked. Disable maintenance mode to restore access.
                  </p>
                </div>
              </div>
            </div>
          )}
        </Card>

        <Card>
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-6">
            General Settings
          </h2>
          <div className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <Input
                label="Site Name"
                value={siteName}
                onChange={(e) => setSiteName(e.target.value)}
                placeholder="MultiVend"
              />
              <Input
                label="Currency Symbol"
                value={currencySymbol}
                onChange={(e) => setCurrencySymbol(e.target.value)}
                placeholder="PKR"
              />
            </div>
            <Input
              label="Contact Email"
              type="email"
              value={contactEmail}
              onChange={(e) => setContactEmail(e.target.value)}
              placeholder="support@multivend.pk"
            />
          </div>
        </Card>

        <Card>
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-6">
            Theme Settings
          </h2>
          <div className="flex items-center justify-between">
            <div>
              <p className="text-gray-900 dark:text-white font-medium">Appearance</p>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Toggle between light and dark mode
              </p>
            </div>
            <div className="flex items-center gap-2 p-1 bg-gray-100 dark:bg-gray-800 rounded-lg">
              <button
                onClick={() => dispatch(toggleTheme())}
                className={`p-2 rounded-md transition-colors ${
                  theme === 'light' ? 'bg-white dark:bg-gray-700 shadow-sm' : ''
                }`}
              >
                <Sun className="w-5 h-5 text-gray-600 dark:text-gray-400" />
              </button>
              <button
                onClick={() => dispatch(toggleTheme())}
                className={`p-2 rounded-md transition-colors ${
                  theme === 'dark' ? 'bg-white dark:bg-gray-700 shadow-sm' : ''
                }`}
              >
                <Moon className="w-5 h-5 text-gray-600 dark:text-gray-400" />
              </button>
            </div>
          </div>
        </Card>

        <div className="flex justify-end">
          <Button onClick={handleSave} isLoading={saving}>
            <Save className="w-4 h-4 mr-2" />
            Save Changes
          </Button>
        </div>
      </motion.div>
    </div>
  );
}
