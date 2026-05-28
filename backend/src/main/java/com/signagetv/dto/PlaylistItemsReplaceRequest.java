package com.signagetv.dto;

import lombok.Data;

import java.util.List;

@Data
public class PlaylistItemsReplaceRequest {
    private List<Item> items;

    @Data
    public static class Item {
        private Long mediaItemId;
        private Integer position;
        private Integer durationSeconds;
    }
}
