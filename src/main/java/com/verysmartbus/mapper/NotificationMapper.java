package com.verysmartbus.mapper;

import com.verysmartbus.dto.response.NotificationResponseDto;
import com.verysmartbus.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponseDto toResponseDto(Notification notification) {
        return new NotificationResponseDto(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getPayloadJson(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
