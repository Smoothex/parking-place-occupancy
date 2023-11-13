import { NgModule } from '@angular/core';
import { RouterModule, Routes, PreloadAllModules } from '@angular/router';
import { MapComponent } from './components/map/map.component';




  
  
const routes: Routes = [
  {
    path: "",
    redirectTo: "map",
    pathMatch: "full",
  }, 
  { path: 'map', component: MapComponent }
  /* {
    path: "map",
    loadChildren: () =>
      import("./components/map/map.component").then((m) => m.MapComponent),
  }, */
 /*  {
    path: 'landing',
    loadChildren: () => import('./pages/landing-page/landing-page.module').then( m => m.LandingPagePageModule)
  },
  */
  
 
];
  
@NgModule({
  imports: [RouterModule.forRoot(routes, { preloadingStrategy: PreloadAllModules })],
  exports: [RouterModule]
})
export class AppRoutingModule { }
