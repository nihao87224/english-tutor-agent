import { createContext, useContext, useMemo, useState, type ReactNode } from "react";

export type Locale = "zh-CN" | "en";
type Messages = Record<string, string>;

const LOCALE_STORAGE_KEY = "englishTutor.web.locale";

const messages: Record<Locale, Messages> = {
  "zh-CN": {
    "app.loading": "正在恢复登录状态...",
    "app.nav.today": "今日练习",
    "app.nav.history": "练习历史",
    "app.nav.account": "账号与额度",
    "app.nav.logout": "退出登录",
    "auth.title": "登录英语表达教练",
    "auth.subtitle": "学习记录、每日额度和练习历史都会绑定到你的邮箱账号。",
    "auth.email": "邮箱",
    "auth.password": "密码",
    "auth.login": "登录",
    "auth.register": "注册",
    "auth.loginTab": "登录",
    "auth.registerTab": "注册",
    "auth.submittingLogin": "登录中...",
    "auth.submittingRegister": "注册中...",
    "auth.switchToRegister": "还没有账号？注册一个",
    "auth.switchToLogin": "已有账号？返回登录",
    "auth.error": "认证失败，请检查邮箱和密码。",
    "onboarding.eyebrow": "首次设置",
    "onboarding.title": "先把表达教练跑起来。",
    "onboarding.summary": "选择目标、每日练习时长和纠错强度。设置会保存到你的账号，换设备登录后也能继续。",
    "onboarding.goal": "目标",
    "onboarding.goal.workplace": "职场",
    "onboarding.goal.workplace.desc": "会议、汇报和工作沟通",
    "onboarding.goal.general": "日常",
    "onboarding.goal.general.desc": "日常表达和自然说法",
    "onboarding.goal.ielts": "雅思",
    "onboarding.goal.ielts.desc": "先练表达基础，后续进入专项",
    "onboarding.minutes": "每日分钟数",
    "onboarding.style": "纠错强度",
    "onboarding.saveRawText": "保存文本用于更好的表达反馈",
    "onboarding.submit": "进入今日教练",
    "onboarding.saving": "保存中...",
    "onboarding.error": "保存失败，请稍后重试。",
    "home.loading.title": "正在加载今日教练",
    "home.loading.desc": "正在读取今天的表达计划...",
    "home.error.title": "今日教练暂不可用",
    "home.retry": "重试",
    "home.empty.title": "今天还没有表达任务",
    "home.empty.desc": "今日计划里暂时没有可练习任务，刷新后再试。",
    "home.refresh": "刷新",
    "home.eyebrow": "今日表达教练",
    "home.start": "开始练习",
    "home.starting": "启动中...",
    "home.plan": "计划",
    "home.quota": "今日额度",
    "home.quotaRemaining": "{remaining} 次可用",
    "home.quotaUnlimited": "无限额度",
    "home.quotaUsed": "已用 {used} / {limit}",
    "home.quotaReset": "重置时间 {time}",
    "coach.back": "返回",
    "coach.task": "今日任务",
    "coach.loadingTask": "加载任务",
    "coach.taskFallback": "使用任务兜底",
    "coach.ready": "已就绪",
    "coach.empty.title": "写一句英文或中英混合想法。",
    "coach.empty.example": "例：I very like this movie because it makes me relax.",
    "coach.retry": "重试",
    "coach.placeholder": "输入你的英文句子或中文想法...",
    "coach.retryPlaceholder": "按建议句型重写一句...",
    "coach.send": "发送",
    "coach.streaming": "生成中...",
    "coach.tryAgain": "再试一次",
    "coach.complete": "完成练习",
    "coach.completing": "完成中...",
    "coach.quotaExceeded": "今日 AI 学习额度已用完，查看历史和总结不会消耗额度。",
    "correction.title": "纠错",
    "correction.upgrades": "表达升级",
    "correction.good": "表达不错",
    "correction.waiting": "等待输入",
    "correction.instead": "不要这样说",
    "correction.say": "可以这样说",
    "correction.why": "原因",
    "correction.natural": "自然表达",
    "correction.pattern": "句型",
    "summary.eyebrow": "每日总结",
    "summary.title": "练习完成。",
    "summary.body": "完成 {tasks} 个任务，产生 {evidence} 条学习证据。",
    "summary.back": "回到今日教练",
    "history.title": "练习历史",
    "history.empty": "完成一次练习后，最近的总结会显示在这里。",
    "history.evidence": "{evidence} 条证据",
    "account.title": "账号与每日额度",
    "account.email": "邮箱",
    "account.status": "状态",
    "account.roles": "角色",
    "account.locale": "界面语言",
    "account.quotaTitle": "今日额度",
    "account.quotaHelp": "只有 AI 生成类学习请求会消耗额度；查看历史、计划和总结不消耗额度。",
    "account.refreshQuota": "刷新额度",
    "account.loadingQuota": "正在加载额度...",
    "account.quotaUnavailable": "额度暂不可用。",
  },
  en: {
    "app.loading": "Restoring your session...",
    "app.nav.today": "Today",
    "app.nav.history": "History",
    "app.nav.account": "Account & quota",
    "app.nav.logout": "Log out",
    "auth.title": "Sign in to English Tutor",
    "auth.subtitle": "Your learning history, daily quota and practice data are tied to your email account.",
    "auth.email": "Email",
    "auth.password": "Password",
    "auth.login": "Log in",
    "auth.register": "Sign up",
    "auth.loginTab": "Log in",
    "auth.registerTab": "Sign up",
    "auth.submittingLogin": "Logging in...",
    "auth.submittingRegister": "Signing up...",
    "auth.switchToRegister": "No account yet? Sign up",
    "auth.switchToLogin": "Already have an account? Log in",
    "auth.error": "Authentication failed. Check your email and password.",
    "onboarding.eyebrow": "First minute setup",
    "onboarding.title": "Set up your expression coach.",
    "onboarding.summary": "Choose your goal, daily practice time and correction style. The setup is saved to your account.",
    "onboarding.goal": "Goal",
    "onboarding.goal.workplace": "Workplace",
    "onboarding.goal.workplace.desc": "Meetings, updates and work communication",
    "onboarding.goal.general": "General",
    "onboarding.goal.general.desc": "Everyday expression and natural phrasing",
    "onboarding.goal.ielts": "IELTS",
    "onboarding.goal.ielts.desc": "Build expression first, then move into exam practice",
    "onboarding.minutes": "Daily minutes",
    "onboarding.style": "Correction style",
    "onboarding.saveRawText": "Save raw text for better expression feedback",
    "onboarding.submit": "Enter today's coach",
    "onboarding.saving": "Saving...",
    "onboarding.error": "Save failed. Please retry later.",
    "home.loading.title": "Loading today's coach",
    "home.loading.desc": "Reading today's expression plan...",
    "home.error.title": "Today's coach is unavailable",
    "home.retry": "Retry",
    "home.empty.title": "No expression task yet",
    "home.empty.desc": "Today's plan does not have a practice task yet. Refresh and try again.",
    "home.refresh": "Refresh",
    "home.eyebrow": "Today's expression coach",
    "home.start": "Start practice",
    "home.starting": "Starting...",
    "home.plan": "Plan",
    "home.quota": "Daily quota",
    "home.quotaRemaining": "{remaining} left",
    "home.quotaUnlimited": "Unlimited",
    "home.quotaUsed": "{used} / {limit} used",
    "home.quotaReset": "Resets at {time}",
    "coach.back": "Back",
    "coach.task": "Today's task",
    "coach.loadingTask": "Loading task",
    "coach.taskFallback": "Task fallback",
    "coach.ready": "Ready",
    "coach.empty.title": "Write one sentence or mixed-language idea.",
    "coach.empty.example": "Example: I very like this movie because it makes me relax.",
    "coach.retry": "Retry",
    "coach.placeholder": "Type your English sentence or Chinese idea...",
    "coach.retryPlaceholder": "Rewrite it using the suggested pattern...",
    "coach.send": "Send",
    "coach.streaming": "Streaming...",
    "coach.tryAgain": "Try Again",
    "coach.complete": "Complete practice",
    "coach.completing": "Completing...",
    "coach.quotaExceeded": "Today's AI learning quota is used up. Reading history and summaries does not consume quota.",
    "correction.title": "Correction",
    "correction.upgrades": "Expression upgrades",
    "correction.good": "Looks good",
    "correction.waiting": "Waiting",
    "correction.instead": "Instead of",
    "correction.say": "Say",
    "correction.why": "Why",
    "correction.natural": "Natural",
    "correction.pattern": "Pattern",
    "summary.eyebrow": "Daily summary",
    "summary.title": "Practice completed.",
    "summary.body": "{tasks} task completed with {evidence} learning evidence item(s).",
    "summary.back": "Back to today's coach",
    "history.title": "Practice history",
    "history.empty": "Recent summaries will appear here after you complete a practice session.",
    "history.evidence": "{evidence} evidence items",
    "account.title": "Account & daily quota",
    "account.email": "Email",
    "account.status": "Status",
    "account.roles": "Roles",
    "account.locale": "Language",
    "account.quotaTitle": "Today's quota",
    "account.quotaHelp": "Only AI generation actions consume quota. Reading history, plans and summaries does not.",
    "account.refreshQuota": "Refresh quota",
    "account.loadingQuota": "Loading quota...",
    "account.quotaUnavailable": "Quota is unavailable.",
  },
};

interface I18nValue {
  locale: Locale;
  setLocale: (locale: Locale) => void;
  t: (key: string, params?: Record<string, string | number>) => string;
}

const I18nContext = createContext<I18nValue | null>(null);

export function I18nProvider({ children, initialLocale }: { children: ReactNode; initialLocale?: string }) {
  const [locale, setLocaleState] = useState<Locale>(() => normalizeLocale(loadStoredLocale() ?? initialLocale ?? navigatorLanguage()));
  const value = useMemo<I18nValue>(
    () => ({
      locale,
      setLocale(nextLocale) {
        setLocaleState(nextLocale);
        saveStoredLocale(nextLocale);
      },
      t(key, params) {
        const template = messages[locale][key] ?? messages.en[key] ?? key;
        return params ? format(template, params) : template;
      },
    }),
    [locale],
  );

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n(): I18nValue {
  const value = useContext(I18nContext);
  if (!value) {
    throw new Error("useI18n must be used inside I18nProvider");
  }
  return value;
}

export function normalizeLocale(locale: string | undefined): Locale {
  return locale?.toLowerCase().startsWith("zh") ? "zh-CN" : "en";
}

function format(template: string, params: Record<string, string | number>): string {
  return template.replace(/\{(\w+)}/g, (_, key: string) => String(params[key] ?? ""));
}

function loadStoredLocale(): string | null {
  return typeof window === "undefined" ? null : window.localStorage.getItem(LOCALE_STORAGE_KEY);
}

function saveStoredLocale(locale: Locale): void {
  if (typeof window !== "undefined") {
    window.localStorage.setItem(LOCALE_STORAGE_KEY, locale);
  }
}

function navigatorLanguage(): string {
  return typeof navigator === "undefined" ? "zh-CN" : navigator.language;
}
