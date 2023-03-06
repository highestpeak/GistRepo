package com.highestpeak.gist.example;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * 一个设计上的例子
 *
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-09
 */
@SuppressWarnings("unused")
public class InputFileExample {

    public static class InputFile {
        private BufferedReader in;

        public InputFile(String fileName) throws Exception {
            //noinspection EmptyFinallyBlock
            try {
                in = new BufferedReader(new FileReader(fileName));
                // Other code that might throw exceptions
            } catch (FileNotFoundException e) {
                System.out.println("Could not open " + fileName);
                // Wasn't open, so don't close it
                throw e;
            } catch (Exception e) {
                // All other exceptions must close it
                try {
                    in.close();
                } catch (IOException e2) {
                    System.out.println("in.close() unsuccessful");
                }
                throw e; // Rethrow
            } finally {
                // Don't close it here!!!
            }
        }

        public String getLine() {
            String s;
            try {
                s = in.readLine();
            } catch (IOException e) {
                throw new RuntimeException("readLine() failed");
            }
            return s;
        }

        public void dispose() {
            try {
                in.close();
                System.out.println("dispose() successful");
            } catch (IOException e2) {
                throw new RuntimeException("in.close() failed");
            }
        }
    }

    /**
     * 相较于上面的 InputFile 的写法。一个更好的实现方式是如果构造函数读取文件并在内部缓冲它. 这样，文件的打开，读取和关闭都发生在构造函数中 <br/>
     * 或者，如果读取和存储文件不切实际，你可以改为生成 Stream。理想情况下. 你可以设计成如下的样子 <br/>
     */
    public static class InputFile2 {
        private final String fileName;

        public InputFile2(String fileName) {
            this.fileName = fileName;
        }

        public Stream<String> getLines() throws IOException {
            return Files.lines(Paths.get(fileName));
        }

        public static void main(String[] args) throws IOException {
            new InputFile2("InputFile2.java").getLines()
                    .skip(15)
                    .limit(1)
                    .forEach(System.out::println);
        }
    }

}
