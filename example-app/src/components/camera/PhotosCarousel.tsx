import React, { useState } from "react";
import { GalleryPhoto } from "@capacitor/camera";
import { Swiper, SwiperSlide } from "swiper/react";
import { Navigation, Pagination } from "swiper/modules";
import PhotoWithMetadata from "./PhotoWithMetadata";
import type { Swiper as SwiperType } from "swiper";

// Import Swiper styles
import "swiper/css";
import "swiper/css/navigation";
import "swiper/css/pagination";

interface IPhotosCarouselProps {
  photos: GalleryPhoto[];
}

const PhotosCarousel: React.FC<IPhotosCarouselProps> = ({ photos }) => {
  const [currentIndex, setCurrentIndex] = useState(1);

  const handleSlideChange = (swiper: SwiperType) => {
    setCurrentIndex(swiper.activeIndex + 1);
  };

  return (
    <div>
      <div style={{ textAlign: "center", padding: "8px", fontWeight: "bold" }}>
        Photo {currentIndex}/{photos.length}
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
        {photos.map((photo, index) => {
          const filePath = photo.path ?? photo.webPath;
          const metadata = photo.exif
            ? JSON.stringify(photo.exif, null, 2)
            : null;

          return (
            <SwiperSlide key={index}>
              <PhotoWithMetadata filePath={filePath} metadata={metadata} />
            </SwiperSlide>
          );
        })}
      </Swiper>
    </div>
  );
};

export default PhotosCarousel;
