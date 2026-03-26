import React, { useState } from "react";
import { MediaResult, MediaType } from "@capacitor/camera";
import { Swiper, SwiperSlide } from "swiper/react";
import { Navigation, Pagination } from "swiper/modules";
import PhotoWithMetadata from "./PhotoWithMetadata";
import VideoWithMetadata from "./VideoWithMetadata";
import type { Swiper as SwiperType } from "swiper";

// Import Swiper styles
import "swiper/css";
import "swiper/css/navigation";
import "swiper/css/pagination";

interface IMediaCarouselProps {
  media: MediaResult[];
  onEditPhoto?: (filePath: string) => void;
}

const MediaCarousel: React.FC<IMediaCarouselProps> = ({ media, onEditPhoto }) => {
  const [currentIndex, setCurrentIndex] = useState(1);

  const handleSlideChange = (swiper: SwiperType) => {
    setCurrentIndex(swiper.activeIndex + 1);
  };

  const isVideo = (item: MediaResult): boolean => {
    // Primary detection: check MediaType
    if (item.type === MediaType.Video) {
      return true;
    }
    if (item.type === MediaType.Photo) {
      return false;
    }

    // Fallback: check format from metadata
    const videoFormats = ['mp4', 'mov', 'avi', 'webm', 'mkv', 'm4v', 'flv'];
    const format = item.metadata?.format;

    if (format) {
      return videoFormats.includes(format.toLowerCase());
    }

    // Last resort: check file extension from path
    const filePath = item.uri ?? item.webPath ?? '';
    const extension = filePath.split('.').pop()?.toLowerCase();
    return extension ? videoFormats.includes(extension) : false;
  };

  return (
    <div>
      <div style={{ textAlign: "center", padding: "8px", fontWeight: "bold" }}>
        Media {currentIndex}/{media.length}
      </div>
      <Swiper
        modules={[Navigation, Pagination]}
        navigation
        pagination={{ clickable: true }}
        spaceBetween={16}
        slidesPerView={1}
        style={{ width: "100%", height: "auto" }}
        onSlideChange={handleSlideChange}
      >
        {media.map((item, index) => {
          const filePath = item.uri ?? item.webPath ?? '';

          return (
            <SwiperSlide key={index}>
              {isVideo(item) ? (
                <VideoWithMetadata
                  filePath={filePath}
                  metadata={item.metadata}
                  thumbnail={item.thumbnail}
                />
              ) : (
                <PhotoWithMetadata
                  filePath={filePath}
                  metadata={item.metadata}
                  onEdit={onEditPhoto}
                />
              )}
            </SwiperSlide>
          );
        })}
      </Swiper>
    </div>
  );
};

export default MediaCarousel;
