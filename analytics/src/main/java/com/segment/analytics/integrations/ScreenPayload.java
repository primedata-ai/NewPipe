/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2014 Segment.io, Inc.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.segment.analytics.integrations;

import static com.segment.analytics.internal.Utils.assertNotNull;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.segment.analytics.Properties;
import com.segment.analytics.internal.Private;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class ScreenPayload extends TrackPayload {

    ScreenPayload(@NonNull String type, @NonNull Date timestamp, @NonNull String sessionId, @Nullable Map<String, Object> target, @Nullable Map<String, Object> source, @Nullable String profileId, @Nullable Map<String, Object> properties) {
        super(type, timestamp, sessionId, target, source, profileId, properties);
    }

    public static TrackPayload.Builder screenBuilder(String name) {
        return new TrackPayload.Builder().event("screen").target(new BasicItemPayload.Builder().itemId("name").itemType("screen").build());
    }
}
