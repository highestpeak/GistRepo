package com.highestpeak.gist.example.base;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Data;

/**
 * @author highestpeak <highestpeak@163.com>
 * Created on 2021-08-23
 * https://stackoverflow.com/questions/39594089/java-8-stream-to-mapinteger-liststring
 */
@SuppressWarnings({"unused", "ResultOfMethodCallIgnored"})
public class JavaStreamExample {

    // object list to map
    // https://stackoverflow.com/questions/32312876/ignore-duplicates-when-producing-map-using-streams
    // https://stackoverflow.com/questions/22635945/adding-up-bigdecimals-using-streams
    // https://stackoverflow.com/questions/599161/best-way-to-convert-an-arraylist-to-a-string
    // https://stackoverflow.com/questions/45418738/remove-duplicate-from-list-java8  java stream list remove duplicates 对于对象来说可能最好是先collect成map
    // https://stackoverflow.com/questions/25903137/java8-hashmapx-y-to-hashmapx-z-using-stream-map-reduce-collector map to map
    // https://stackoverflow.com/questions/33138577/how-to-convert-list-to-map-with-indexes-using-stream-java-8
    // https://stackoverflow.com/questions/30125296/how-to-sum-a-list-of-integers-with-java-streams
    // https://stackoverflow.com/questions/16748470/how-to-convert-a-list-to-variable-argument-parameter-java
    // https://stackoverflow.com/questions/40772997/how-to-convert-listv-into-mapk-listv-with-java-8-streams-and-custom-list
    // https://stackoverflow.com/questions/25147094/how-can-i-turn-a-list-of-lists-into-a-list-in-java-8
    // https://stackoverflow.com/questions/49029441/grouping-by-and-map-value/49029481#49029481
    // https://stackoverflow.com/questions/23213891/how-to-map-values-in-a-map-in-java-8
    // test
    // https://stackoverflow.com/questions/22635945/adding-up-bigdecimals-using-streams
    // https://stackoverflow.com/questions/23699371/java-8-distinct-by-property
    // https://stackoverflow.com/questions/30125296/how-to-sum-a-list-of-integers-with-java-streams
    // https://stackoverflow.com/questions/33138577/how-to-convert-list-to-map-with-indexes-using-stream-java-8

    // https://stackoverflow.com/questions/41997271/how-do-i-get-comparator-comparing-to-correctly-infer-type-parameters
    // https://blog.csdn.net/weixin_41453111/article/details/104942362

    // List<IdentityVO> kuimResult = kuimClients.stream()
    //         .map(IDaaSOpenClient::getAdminIdentityOpenClient)
    //         .flatMap(client ->
    //                 // 分批 100 一组去查询
    //                 Lists.partition(kuimUserIds, 100)
    //                         .stream()
    //                         .map(partitionKuimUserIds -> batchQueryIdentityById(client, partitionKuimUserIds))
    //         )
    //         .filter(Objects::nonNull)
    //         // 每个用户的联系方式
    //         .flatMap(result -> Optional.ofNullable(result.getResult())
    //                 .map(Collection::stream)
    //                 .orElse(Stream.empty())
    //         )
    //         .collect(Collectors.toList());

    // java stream map ignore exception
    // https://stackoverflow.com/questions/19757300/java-8-lambda-streams-filter-by-method-with-exception

    public static void main(String[] args) {
        // 将一个 object's list 中两个 field map 到一个 list

    }

    /**
     * 测试类 商品
     */
    @Data
    public static class MallGoods {
        private String goodsTitle;
        private String goodsDesc;
        private LocalDateTime createTime;
        private LocalDateTime modifyTime;
    }

    /**
     * 将一个 object's list 中两个 field map 到一个 list <br/>
     * https://stackoverflow.com/questions/33603486/how-to-collect-two-fields-of-an-object-into-the-same-list
     */
    public static void twoFieldIntoSameList(List<MallGoods> goodsList) {
        goodsList.stream()
                .flatMap(mallGoods -> Stream.of(mallGoods.getCreateTime(), mallGoods.getModifyTime()))
                .collect(Collectors.toList());
    }

    /**
     * collect to map 时, 对重复项的处理 <br/>
     */
    public static void collectToMapProcessDuplicate() {

    }


}
