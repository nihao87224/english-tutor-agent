export function createRefreshCoordinator(refresh: () => Promise<boolean>): () => Promise<boolean> {
  let pendingRefresh: Promise<boolean> | undefined;

  return () => {
    if (!pendingRefresh) {
      pendingRefresh = refresh().finally(() => {
        pendingRefresh = undefined;
      });
    }
    return pendingRefresh;
  };
}
