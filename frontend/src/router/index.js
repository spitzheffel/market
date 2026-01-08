import { createRouter, createWebHistory } from 'vue-router'
import ShellLayout from '../views/ShellLayout.vue'
import Home from '../views/Home.vue'
import HomeRight from '../views/home/HomeRight.vue'
import HomeBottom from '../views/home/HomeBottom.vue'
import Markets from '../views/Markets.vue'
import Watchlist from '../views/Watchlist.vue'
import Engine from '../views/Engine.vue'
import Signals from '../views/Signals.vue'
import Strategy from '../views/Strategy.vue'
import Backtest from '../views/Backtest.vue'
import AutoTrade from '../views/AutoTrade.vue'
import Settings from '../views/Settings.vue'

const routes = [
  {
    path: '/',
    component: ShellLayout,
    children: [
      {
        path: '',
        name: 'Home',
        components: { default: Home, right: HomeRight, bottom: HomeBottom },
        meta: { section: 'home', hasRight: true, hasBottom: true }
      },
      {
        path: 'markets',
        name: 'Markets',
        component: Markets,
        meta: { section: 'markets' }
      },
      {
        path: 'watchlist',
        name: 'Watchlist',
        component: Watchlist,
        meta: { section: 'watchlist' }
      },
      {
        path: 'engine',
        name: 'Engine',
        component: Engine,
        meta: { section: 'engine' }
      },
      {
        path: 'signals',
        name: 'Signals',
        component: Signals,
        meta: { section: 'signals' }
      },
      {
        path: 'strategy',
        name: 'Strategy',
        component: Strategy,
        meta: { section: 'strategy' }
      },
      {
        path: 'backtest',
        name: 'Backtest',
        component: Backtest,
        meta: { section: 'backtest' }
      },
      {
        path: 'autotrade',
        name: 'AutoTrade',
        component: AutoTrade,
        meta: { section: 'autotrade' }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: Settings,
        meta: { section: 'settings' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
