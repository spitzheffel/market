import { createRouter, createWebHistory } from 'vue-router'
import ChanDashboard from '../views/ChanDashboard.vue'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: ChanDashboard,
    meta: { section: 'home' }
  },
  {
    path: '/markets',
    name: 'Markets',
    component: ChanDashboard,
    meta: { section: 'markets' }
  },
  {
    path: '/watchlist',
    name: 'Watchlist',
    component: ChanDashboard,
    meta: { section: 'watchlist' }
  },
  {
    path: '/engine',
    name: 'Engine',
    component: ChanDashboard,
    meta: { section: 'engine' }
  },
  {
    path: '/signals',
    name: 'Signals',
    component: ChanDashboard,
    meta: { section: 'signals' }
  },
  {
    path: '/strategy',
    name: 'Strategy',
    component: ChanDashboard,
    meta: { section: 'strategy' }
  },
  {
    path: '/backtest',
    name: 'Backtest',
    component: ChanDashboard,
    meta: { section: 'backtest' }
  },
  {
    path: '/autotrade',
    name: 'AutoTrade',
    component: ChanDashboard,
    meta: { section: 'autotrade' }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: ChanDashboard,
    meta: { section: 'settings' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
