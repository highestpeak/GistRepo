调用方式: `Hconfs.ofInteger(...).build();`

实现方式:

1. SPI 机制. 找到 `com.highestpeak.hconf.client.HconfFactory` 的实现类
2. 各个接口实现
3. 对于外界来说，只需要使用 Hconfs、Hconf
