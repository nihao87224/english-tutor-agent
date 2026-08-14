import { describe, expect, it } from "vitest";
import { normalizeLocale } from "./i18n";

describe("normalizeLocale", () => {
  it("normalizes Chinese variants to zh-CN", () => {
    expect(normalizeLocale("zh-CN")).toBe("zh-CN");
    expect(normalizeLocale("zh-Hans")).toBe("zh-CN");
  });

  it("uses English for missing or non-Chinese locales", () => {
    expect(normalizeLocale(undefined)).toBe("en");
    expect(normalizeLocale("en-US")).toBe("en");
  });
});
