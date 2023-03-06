package com.highestpeak.gist.redis.util.cache.objects;

import com.google.protobuf.Message;

public interface ProtoObject {
    Message toProto();
}
