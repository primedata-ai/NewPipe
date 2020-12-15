package org.schabi.newpipe.util;

import com.segment.analytics.Analytics;
import com.segment.analytics.integrations.BasicItemPayload;
import com.segment.analytics.internal.Utils;

import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.player.resolver.MediaSourceTag;

import java.util.Map;

public class MetadataTracking {
    public static final String PLAY = "play";
    public static final String PAUSE = "pause";
    public static final String SEEK = "seek";
    public static final String PLAY_NEXT = "play_next";

    public static BasicItemPayload getMetadataTrack(MediaSourceTag tag) {
        BasicItemPayload.Builder builder = new BasicItemPayload.Builder();
        StreamInfo info = tag.getMetadata();
        builder.itemId(info.getId()).itemType("video");

        Map<String, Object> map = new Utils.NullableConcurrentHashMap<>();
        if (info.getCategory().length() > 0)
            map.put("category", info.getCategory());
        map.put("likes", info.getLikeCount());
        if (info.getSubChannelName().length() > 0)
            map.put("channel", info.getSubChannelName());
        map.put("views", info.getViewCount());
        map.put("type", info.getStreamType().name());
        map.put("title", info.getName());

        return builder.properties(map).build();
    }
}
