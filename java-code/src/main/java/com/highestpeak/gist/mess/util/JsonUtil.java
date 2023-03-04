package com.highestpeak.gist.mess.util;

import static com.fasterxml.jackson.core.JsonFactory.Feature.INTERN_FIELD_NAMES;
import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_COMMENTS;
import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

import java.io.IOException;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.highestpeak.gist.common.WarmupAble;
import com.highestpeak.gist.common.help.JacksonException;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;

/**
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-03
 */
@SuppressWarnings("unused")
public class JsonUtil implements WarmupAble {

    // todo 可以写一个 warmup ，因为老是直接创建，可能降低 util 类库的加载速度

    // todo 使用一个 supplier
    private static final ObjectMapper MAPPER;

    @Override
    public void tryWarmup() {

    }

    // todo Gson 维护到这里
    // private Gson gson = new GsonBuilder().create();

    /*
     * 有写可能会在 spring IOC 容器没有设置完成前调用 ObjectMapper
     */
    static {
        // disable INTERN_FIELD_NAMES, 解决GC压力大、内存泄露的问题
        // https://jira.corp.kuaishou.com/browse/INFRAJAVA-552
        MAPPER = new ObjectMapper(new JsonFactory().disable(INTERN_FIELD_NAMES));
        MAPPER.registerModule(new GuavaModule());

        MAPPER.disable(FAIL_ON_UNKNOWN_PROPERTIES);
        //MAPPER.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        MAPPER.enable(ALLOW_UNQUOTED_CONTROL_CHARS);
        MAPPER.enable(ALLOW_COMMENTS);

        // 不配置 ParameterNamesModule 则默认行为是 noArgConstructor + setter 其他情况都解析不了，配置了则可以再兼容 allConstructor 解析
        MAPPER.registerModule(new ParameterNamesModule());
        MAPPER.registerModule(new KotlinModule());
        MAPPER.registerModule(new ProtobufModule());
    }

    /**
     * 向 spring IOC 容器注册 static 的 MAPPER
     * 这个地方不使用 primary 因为每个模块可能自己写自己的 mapper 的 config 的 bean
     */
    @Bean
    // @Primary
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper jacksonObjectMapper() {
        return MAPPER;
    }

    // 适用于各自模块自定义 Spring MVC 解析等情况:
    // 需要自定义 serializer 和 deserializer 的模块,可以通过下述方法,拿到 mapper 然后自定义
    // @Component
    // public class JsonUtil implements BeanPostProcessor {
    // @Autowired
    // public void setObjectMapper(ObjectMapper objectMapper) {
    //     JsonUtil.objectMapper = objectMapper;
    //     you code here to config serializer & deserializer
    // }
    // }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    // ============================================================== toJson ============================================================== //

    public static String toJson(@Nullable Object obj) {
        return toJson(obj, false);
    }

    public static String toJson(@Nullable Object obj, boolean emptyIfException) {
        if (obj == null) {
            return emptyIfException ? StringUtils.EMPTY : null;
        }
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            if (emptyIfException) {
                return StringUtils.EMPTY;
            }
            // todo 要不要抛出一个自定义的异常
            // throw new UncheckedJsonProcessingException(e);
            throw new JacksonException(e, obj);
        }
    }

    // ============================================================== fromJson ============================================================== //

    public static <T> T fromJson(String json, Class<T> tClass) {
        try {
            return MAPPER.readValue(json, tClass);
        } catch (IOException e) {
            throw new JacksonException(e, json);
        }
    }

    public static <T> T fromJson(String json, TypeReference<T> tTypeReference) {
        try {
            return MAPPER.readValue(json, tTypeReference);
        } catch (IOException e) {
            throw new JacksonException(e, json);
        }
    }

    public static <T> T fromJson(String json, TypeReference<T> tTypeReference, boolean nullIfFailed) {
        try {
            return MAPPER.readValue(json, tTypeReference);
        } catch (IOException e) {
            if (nullIfFailed) {
                return null;
            }
            throw new JacksonException(e, json);
        }
    }

    public static <T> T fromJson(String json, JavaType valueType) {
        try {
            return MAPPER.readValue(json, valueType);
        } catch (IOException e) {
            throw new JacksonException(e, json);
        }
    }

    public static JsonNode fromJsonReadTree(String content) throws IOException {
        return MAPPER.readTree(content);
    }

}
