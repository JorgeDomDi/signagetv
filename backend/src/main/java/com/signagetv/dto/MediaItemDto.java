package com.signagetv.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaItemDto {
    private Long id;
    private String filename;
    private String type;            // IMAGE | VIDEO
    private String mimeType;
    private Long sizeBytes;
    private Integer durationSeconds;
    private String url;             // URL absoluta para descarga
    private LocalDateTime createdAt;
}
