一些第三方组件或者第三方中间件的MOCK接口，例如Redis的MOCK接口
不去引入 redis、kafka、mysql 等组件依赖，单纯写一个门面模式/外观模式API，假装操作了对应的组件