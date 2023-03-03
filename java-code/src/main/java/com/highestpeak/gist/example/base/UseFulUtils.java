package com.highestpeak.gist.example.base;

/**
 * @author highestpeak <highestpeak@163.com>
 * Created on 2022-05-16
 * 记录一些有用的util类
 */
public class UseFulUtils {
    public static void main(String[] args) {
        // CheckIfApiOperation 特定user 对 特定api 是否有操作权限
        // 正则表达式检查中英文
        // 记录api操作日志
        // 统一的api处理，Result<T>，ResultPage<T>这样的东西
        // 统一的异常处理，统一的异常拦截处理
        // 游标形式的分页查询
        // 次数的查询这样的
        // 按业务限流
        // 按用户、按部门、按群组这样的灰度信息
        // batchExecutor

        // randomSelectUtil in collection map

        // 项目的BlockingDelayBufferTrigger 以及公司的 BufferTrigger
        // 亲缘性线程池


        // 这个里面的一些什么 list of object to map 的工具类，应该是可以按需引入的，最好是每个项目自己写一个自己的工具类，不要用这个类库里的
        // 其实也不多，就几个
        // https://docs.corp.kuaishou.com/k/home/VJqep9fhId4c/fcADlGQXPiJ7tbV1Ed-q15ba0ca
        // https://git.corp.kuaishou.com/server/kuaishou-webservice-arch/-/tree/master/kuaishou-server-util/src/main/java/com/kuaishou/server/util

        // http 的 util
        // 获取 header 头部
        // 获取 url 中的 param 参数 parse
        // java proto to map

        // 重试代码 https://kstack.corp.kuaishou.com/article/4332 + https://github.com/rholder/guava-retrying

        // lombok 自定义注解
        // https://github.com/projectlombok/lombok/issues/2855#issuecomment-849625842
        // https://github.com/kokorin/lombok-presence-checker
        // https://github.com/sympower/symbok
        // 一个 class 所有字段变成一个 map: Map<fieldName, fieldValueGetFunc> 然后可以根据这个 map 来遍历

        // 需要一个这样的方法：即能够从 map 中一次拿去多个 key 的值,并且如果全都没有值的话可以给一个默认值
        // map.getOrDefault(key1, key2, key3, defaultValueIfAllKeyNotExist).foreach(value -> ... )

        // 如何不在 stream...foreach(...) 这种中写 try-catch 来处理 exception 而是直接类似 seakThrow 这种来处理 https://stackoverflow.com/questions/23548589/java-8-how-do-i-work-with-exception-throwing-methods-in-streams


        // spring 项目基础的能力：
        // 动态配置（轻量级类似 hconf 几个类就可以完成的）
        // mybatis orm 框架
        // json objectMapper factory
        // 动态日志框架
        // businessException(runtimeException) errorhandler logback


        // stopWatch 耗时统计

        // 获取 dbVersionTime 的下一个整点，例如 dbVersionTime 是 "2022110911" 则返回值是 "2022110912" 即 11 点的下一个整点是 12 点
        // dbVersionTime.toLocalDate()
        //         .atTime(dbVersionTime.plusHours(1).getHour(), 0, 0);
    }
}
