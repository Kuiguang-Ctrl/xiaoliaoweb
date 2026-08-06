import { createRouter, createWebHistory } from 'vue-router'
import homePage from '../views/home_page/home_page.vue'
import signin from '../views/signin/signin.vue'
import detailPage from '../views/DetailPage.vue'
import brainGameDetail from '../views/BrainGameDetail.vue'
import brainGamePlay from '../views/BrainGamePlay.vue'
import FlipCard from '@/games/FlipCard.vue'
import FindRule from '@/games/FindRule.vue'
import WordGame from '@/games/WordGame.vue'
import FindDiff from '@/games/FindDiff.vue'
import profile from '../views/profile/profile.vue'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: homePage,
  },
  {
    path: '/signin',
    name: 'Signin',
    component: signin,
  },
  {
    path: '/detail/:id',
    name: 'Detail',
    component: detailPage,
    props: true,
  },
  {
    path: '/brain-game',
    name: 'BrainGameDetail',
    component: brainGameDetail,
  },
  {
    path: '/brain-game/play',
    name: 'BrainGamePlay',
    component: brainGamePlay,
  },
  {
    path: '/brain-game/play/flip',
    name: 'FlipCard',
    component: FlipCard,
  },
  {
    path: '/brain-game/play/rule',
    name: 'FindRule',
    component: FindRule,
  },
  {
    path: '/brain-game/play/word',
    name: 'WordGame',
    component: WordGame,
  },
  {
    path: '/brain-game/play/diff',
    name: 'FindDiff',
    component: FindDiff,
  },
  {
    path: '/profile',
    name: 'Profile',
    component: profile,
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router