import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { initAccessibility } from './utils/accessibility'

initAccessibility()

const app = createApp(App)
app.use(router)
app.mount('#app')
