import React, { useState } from "react";
import { GalleryPhoto } from "@capacitor/camera";
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
  // TODO change this type to allow including future MediaResult in ChooseFromGallery
  media: GalleryPhoto[];
  onEditPhoto?: (filePath: string) => void;
}

const MediaCarousel: React.FC<IMediaCarouselProps> = ({ media, onEditPhoto }) => {
  const [currentIndex, setCurrentIndex] = useState(1);

  const handleSlideChange = (swiper: SwiperType) => {
    setCurrentIndex(swiper.activeIndex + 1);
  };

  const isVideo = (format: string | undefined, filePath: string): boolean => {
    const videoFormats = ['mp4', 'mov', 'avi', 'webm', 'mkv', 'm4v', 'flv'];

    // Check format first if available
    if (format) {
      return videoFormats.includes(format.toLowerCase());
    }

    // Fall back to checking file extension from path
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
          const filePath = item.path ?? item.webPath;
          const metadata = item.exif
            ? JSON.stringify(item.exif, null, 2)
            : null;

          return (
            <SwiperSlide key={index}>
              {isVideo(item.format, filePath) ? (
                <VideoWithMetadata filePath={filePath} metadata={metadata} />
              ) : (
                <PhotoWithMetadata
                  filePath={filePath}
                  metadata={metadata}
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
