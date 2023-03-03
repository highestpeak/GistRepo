package com.highestpeak.gist.concurrent.util;

/**
 * WARNING!!! 这一个非安全初始化的例子 WARNING!!!
 * <p>
 * 如果一个线程调用initialize方法，而另一个调用getHolder方法，
 * 则第二个线程可以观察这几种情况之一：
 * 1. holder的引用为空
 * 2. 完全实例化的Holder对象中的n为42
 * 3. 具有未初始化n的部分初始化的Holder对象，其中包含字段的n默认值0
 * <p>
 * 原因是 JMM允许编译器在初始化新的Holder对象之前为新的Holder对象分配内存，并将对该内存的引用分配给holder字段，
 * 换句话说，编译器可以对holder实例字段的写入和初始化Holder对象的写入（即this.n = n）进行重排序，
 * https://www.cnblogs.com/CreateMyself/p/12459141.html
 */
public class NotSafeInitialize {
    private class Foo {
        private Holder holder;

        public Holder getHolder() {
            return holder;
        }

        public void initialize() {
            holder = new Holder(42);
        }
    }

    private class Holder {
        private int n;

        public Holder(int n) {
            this.n = n;
        }
    }
}
