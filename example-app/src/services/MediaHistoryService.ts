export interface MediaHistoryItem {
  id: string;
  timestamp: number;
  mediaType: "photo" | "video";
  method: string;
  path: string;
  webPath: string;
  format?: string;
  size?: number;
  duration?: number;
  saved?: boolean;
}

class MediaHistoryServiceClass {
  private readonly STORAGE_KEY = "capacitor_camera_media_history";
  private readonly MAX_ITEMS = 100;

  addMedia(item: Omit<MediaHistoryItem, "id" | "timestamp">): void {
    try {
      const timestamp = Date.now();
      const id = `${timestamp}-${Math.random().toString(36).substring(2, 9)}`;

      const newItem: MediaHistoryItem = {
        ...item,
        id,
        timestamp,
      };

      const history = this.getAllMedia();
      history.unshift(newItem);

      const trimmedHistory = history.slice(0, this.MAX_ITEMS);

      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(trimmedHistory));
    } catch (error) {
      console.error("Failed to add media to history:", error);
    }
  }

  getAllMedia(): MediaHistoryItem[] {
    try {
      const data = localStorage.getItem(this.STORAGE_KEY);
      if (!data) {
        return [];
      }
      return JSON.parse(data);
    } catch (error) {
      console.error("Failed to retrieve media history:", error);
      return [];
    }
  }

  clearHistory(): void {
    try {
      localStorage.removeItem(this.STORAGE_KEY);
    } catch (error) {
      console.error("Failed to clear media history:", error);
    }
  }

  getCount(): number {
    return this.getAllMedia().length;
  }
}

export const MediaHistoryService = new MediaHistoryServiceClass();
