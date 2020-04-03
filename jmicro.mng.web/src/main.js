import Vue from 'vue'
import App from './App.vue'
import VueRouter from 'vue-router'

import iView from 'view-design'
import 'view-design/dist/styles/iview.css'

import JConfig from './components/config/JConfig.vue'
import JService from './components/service/JService.vue'
import JRouter from './components/route/JRouter.vue'
import JShell from './components/shell/JShell.vue'
import JLog from './components/log/JLog.vue'
import JStatis from './components/statis/JStatis.vue'

Vue.use(iView)
Vue.use(VueRouter)
Vue.use(window.jm)

Vue.config.productionTip = false

const routes = [
    { path: '/config', component: JConfig },
    { path: '/router', component: JRouter },
    { path: '/shell', component: JShell },
    { path: '/log', component: JLog },
    { path: '/statisService', component: JStatis },
    { path: '/', component: JService },


];

const router = new VueRouter({
    routes // short for `routes: routes`
})


window.jm.vue = new Vue({
    render: h => h(App),
    router,
});

//window.vue = window.jm.vue;
//window.vue.jm = window.jm;

window.jm.vue.$mount('#app')

