package org.example.api.dto.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.channel.ChannelInfoItem;
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamType;

/**
 * Data Transfer Object for individual search result items.
 * Handles different types of search results (videos, channels, playlists).
 */
public record SearchItemDTO(
        String type, // "stream", "channel", "playlist"
        String name,
        String url,
        String thumbnailUrl,
        // Stream-specific
        String uploaderName,
        String uploaderUrl,
        String uploaderAvatarUrl,
        Boolean uploaderVerified,
        Long duration,
        Long viewCount,
        String uploadDate,
        String streamType,
        @JsonProperty("isShortFormContent") Boolean isShortFormContent,
        // Channel-specific
        Long subscriberCount,
        Long streamCount,
        String description,
        // Playlist-specific
        String playlistType,
        Long videoCount) {

    /** Duplicate key the old bean emitted through getShortFormContent(); kept so the JSON is unchanged. */
    @JsonProperty(value = "shortFormContent", access = JsonProperty.Access.READ_ONLY)
    public Boolean shortFormContent() {
        return isShortFormContent;
    }

    public static SearchItemDTO from(InfoItem item) {
        Builder builder = builder().name(item.getName()).url(item.getUrl());

        if (!item.getThumbnails().isEmpty()) {
            builder.thumbnailUrl(item.getThumbnails().getFirst().getUrl());
        }

        switch (item) {
            case StreamInfoItem streamItem -> mapStreamInfo(builder.type("stream"), streamItem);
            case ChannelInfoItem channelItem -> mapChannelInfo(builder.type("channel"), channelItem);
            case PlaylistInfoItem playlistItem -> mapPlaylistInfo(builder.type("playlist"), playlistItem);
            default -> {}
        }

        return builder.build();
    }

    private static void mapStreamInfo(Builder builder, StreamInfoItem streamItem) {
        builder.uploaderName(streamItem.getUploaderName())
                .uploaderUrl(streamItem.getUploaderUrl())
                .uploaderVerified(streamItem.isUploaderVerified())
                .duration(streamItem.getDuration())
                .viewCount(streamItem.getViewCount())
                .isShortFormContent(streamItem.isShortFormContent());

        if (!streamItem.getUploaderAvatars().isEmpty()) {
            builder.uploaderAvatarUrl(streamItem.getUploaderAvatars().getFirst().getUrl());
        }
        if (streamItem.getUploadDate() != null) {
            builder.uploadDate(streamItem.getUploadDate().offsetDateTime().toString());
        }
        StreamType streamType = streamItem.getStreamType();
        if (streamType != null) {
            builder.streamType(streamType.name());
        }
    }

    private static void mapChannelInfo(Builder builder, ChannelInfoItem channelItem) {
        builder.subscriberCount(channelItem.getSubscriberCount())
                .streamCount(channelItem.getStreamCount())
                .description(channelItem.getDescription())
                .uploaderVerified(channelItem.isVerified());
    }

    private static void mapPlaylistInfo(Builder builder, PlaylistInfoItem playlistItem) {
        builder.uploaderName(playlistItem.getUploaderName())
                .uploaderUrl(playlistItem.getUploaderUrl())
                .videoCount(playlistItem.getStreamCount());

        if (playlistItem.getPlaylistType() != null) {
            builder.playlistType(playlistItem.getPlaylistType().name());
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String type;
        private String name;
        private String url;
        private String thumbnailUrl;
        private String uploaderName;
        private String uploaderUrl;
        private String uploaderAvatarUrl;
        private Boolean uploaderVerified;
        private Long duration;
        private Long viewCount;
        private String uploadDate;
        private String streamType;
        private Boolean isShortFormContent;
        private Long subscriberCount;
        private Long streamCount;
        private String description;
        private String playlistType;
        private Long videoCount;

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder thumbnailUrl(String thumbnailUrl) {
            this.thumbnailUrl = thumbnailUrl;
            return this;
        }

        public Builder uploaderName(String uploaderName) {
            this.uploaderName = uploaderName;
            return this;
        }

        public Builder uploaderUrl(String uploaderUrl) {
            this.uploaderUrl = uploaderUrl;
            return this;
        }

        public Builder uploaderAvatarUrl(String uploaderAvatarUrl) {
            this.uploaderAvatarUrl = uploaderAvatarUrl;
            return this;
        }

        public Builder uploaderVerified(Boolean uploaderVerified) {
            this.uploaderVerified = uploaderVerified;
            return this;
        }

        public Builder duration(Long duration) {
            this.duration = duration;
            return this;
        }

        public Builder viewCount(Long viewCount) {
            this.viewCount = viewCount;
            return this;
        }

        public Builder uploadDate(String uploadDate) {
            this.uploadDate = uploadDate;
            return this;
        }

        public Builder streamType(String streamType) {
            this.streamType = streamType;
            return this;
        }

        public Builder isShortFormContent(Boolean isShortFormContent) {
            this.isShortFormContent = isShortFormContent;
            return this;
        }

        public Builder subscriberCount(Long subscriberCount) {
            this.subscriberCount = subscriberCount;
            return this;
        }

        public Builder streamCount(Long streamCount) {
            this.streamCount = streamCount;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder playlistType(String playlistType) {
            this.playlistType = playlistType;
            return this;
        }

        public Builder videoCount(Long videoCount) {
            this.videoCount = videoCount;
            return this;
        }

        public SearchItemDTO build() {
            return new SearchItemDTO(
                    type,
                    name,
                    url,
                    thumbnailUrl,
                    uploaderName,
                    uploaderUrl,
                    uploaderAvatarUrl,
                    uploaderVerified,
                    duration,
                    viewCount,
                    uploadDate,
                    streamType,
                    isShortFormContent,
                    subscriberCount,
                    streamCount,
                    description,
                    playlistType,
                    videoCount);
        }
    }
}
