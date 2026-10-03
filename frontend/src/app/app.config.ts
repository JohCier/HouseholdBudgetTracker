import { registerLocaleData } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import localeDe from '@angular/common/locales/de';
import { ApplicationConfig, LOCALE_ID, provideBrowserGlobalErrorListeners } from '@angular/core';

registerLocaleData(localeDe);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(),
    { provide: LOCALE_ID, useValue: 'de-DE' },
  ],
};
