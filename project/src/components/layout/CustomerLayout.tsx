import { Outlet } from 'react-router-dom';
import Navbar from './CustomerNavbar';
import Footer from './Footer';

export default function CustomerLayout() {
  return (
    <div className="min-h-screen flex flex-col bg-gray-50 dark:bg-gray-950">
      <Navbar />
      <main className="flex-1">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}
