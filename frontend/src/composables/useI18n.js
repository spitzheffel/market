import { useI18n as vueUseI18n } from 'vue-i18n';

export function useI18n() {
  const { t, locale } = vueUseI18n();

  const setLocale = (newLocale) => {
    locale.value = newLocale;
    localStorage.setItem('locale', newLocale);
  };

  const availableLocales = [
    { value: 'zh-CN', label: '简体中文' },
    { value: 'en-US', label: 'English' },
  ];

  return {
    t,
    locale,
    setLocale,
    availableLocales,
  };
}
