import React, { useState, useEffect } from "react";
import { IonCard, IonCardContent, IonCardHeader, IonCardTitle } from "@ionic/react";
import { Swiper, SwiperSlide } from "swiper/react";
import { Navigation, Pagination } from "swiper/modules";
import type { Swiper as SwiperType } from "swiper";
import { TEST_IMAGES, TestImage } from "./TestImageData";

import "swiper/css";
import "swiper/css/navigation";
import "swiper/css/pagination";

interface TestImageSelectorProps {
  onImageSelected: (image: TestImage) => void;
}

const TestImageSelector: React.FC<TestImageSelectorProps> = ({ onImageSelected }) => {
  const [currentIndex, setCurrentIndex] = useState(0);

  useEffect(() => {
    onImageSelected(TEST_IMAGES[0]);
  }, []);

  const handleSlideChange = (swiper: SwiperType): void => {
    const index = swiper.activeIndex;
    setCurrentIndex(index);
    onImageSelected(TEST_IMAGES[index]);
  };

  const getImageSrc = (image: TestImage): string => {
    if (image.type === "base64") {
      return `data:image/png;base64,${image.data}`;
    }
    return image.data;
  };

  return (
    <IonCard>
      <IonCardHeader>
        <IonCardTitle>Select Test Image</IonCardTitle>
      </IonCardHeader>
      <IonCardContent>
        <div style={{ textAlign: "center", padding: "8px", fontWeight: "bold" }}>
          {currentIndex + 1}/{TEST_IMAGES.length} - {TEST_IMAGES[currentIndex].name}
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
          {TEST_IMAGES.map((image, index) => (
            <SwiperSlide key={image.id}>
              <div
                style={{
                  display: "flex",
                  justifyContent: "center",
                  alignItems: "center",
                  padding: "16px",
                  minHeight: "200px",
                  backgroundColor: "#f5f5f5",
                }}
              >
                <img
                  src={getImageSrc(image)}
                  alt={image.name}
                  style={{
                    maxWidth: "100%",
                    maxHeight: "300px",
                    objectFit: "contain",
                  }}
                />
              </div>
            </SwiperSlide>
          ))}
        </Swiper>
      </IonCardContent>
    </IonCard>
  );
};

export default TestImageSelector;
