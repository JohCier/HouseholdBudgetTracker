import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import localeDe from '@angular/common/locales/de';
import {
  ApplicationConfig,
  LOCALE_ID,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { routes } from './app.routes';
import { sessionExpiredInterceptor } from './auth/session-expired.interceptor';
import { registerIcons } from './icons';

registerLocaleData(localeDe);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(withInterceptors([sessionExpiredInterceptor])),
    provideRouter(routes, withComponentInputBinding()),
    provideAppInitializer(registerIcons),
    { provide: LOCALE_ID, useValue: 'de-DE' },
  ],
};
