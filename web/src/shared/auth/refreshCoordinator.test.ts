import { describe, expect, it, vi } from "vitest";
import { createRefreshCoordinator } from "./refreshCoordinator";

describe("createRefreshCoordinator", () => {
  it("shares one in-flight refresh across concurrent unauthorized requests", async () => {
    const refreshResolvers: Array<(value: boolean) => void> = [];
    const refresh = vi.fn(
      () => new Promise<boolean>((resolve) => {
        refreshResolvers.push(resolve);
      }),
    );
    const coordinatedRefresh = createRefreshCoordinator(refresh);

    const first = coordinatedRefresh();
    const second = coordinatedRefresh();

    expect(refresh).toHaveBeenCalledTimes(1);
    refreshResolvers[0](true);
    await expect(Promise.all([first, second])).resolves.toEqual([true, true]);

    const third = coordinatedRefresh();
    expect(refresh).toHaveBeenCalledTimes(2);
    refreshResolvers[1](true);
    await expect(third).resolves.toBe(true);
  });
});
