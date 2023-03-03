import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * @author zhangjike <zhangjike03@kuaishou.com>
 * Created on 2022-08-31
 */
public class IELTSSpider {

    public static void main(String[] args) throws IOException {
        List<String> pageUrlList = Lists.newArrayList(
                "https://ieltstrainingonline.com/audioscripts-cambridge-ielts-17-listening-test-01/",
                "https://ieltstrainingonline.com/audioscripts-cambridge-ielts-17-listening-test-02/",
                "https://ieltstrainingonline.com/audioscripts-cambridge-ielts-17-listening-test-03/",
                "https://ieltstrainingonline.com/audioscripts-cambridge-ielts-17-listening-test-04/",

                "https://ieltstrainingonline.com/audio-script-cambridge-ielts-16-listening-test-01/",
                "https://ieltstrainingonline.com/audio-script-cambridge-ielts-16-listening-test-02/",
                "https://ieltstrainingonline.com/audio-script-cambridge-ielts-16-listening-test-03/",
                "https://ieltstrainingonline.com/audio-script-cambridge-ielts-16-listening-test-04/",

                "https://ieltstrainingonline.com/audio-script-cambridge-ielts-15-listening-test-01/",
                "https://ieltstrainingonline.com/audio-script-cambridge-ielts-15-listening-test-02/",
                "https://ieltstrainingonline.com/audio-script-cambridge-ielts-15-listening-test-03/",
                "https://ieltstrainingonline.com/audio-script-cambridge-ielts-15-listening-test-04/",

                "https://ieltstrainingonline.com/transcript-cambridge-ielts-14-listening-test-01/",
                "https://ieltstrainingonline.com/transcript-cambridge-ielts-14-listening-test-02/",
                "https://ieltstrainingonline.com/transcript-cambridge-ielts-14-listening-test-03/",
                "https://ieltstrainingonline.com/transcript-cambridge-ielts-14-listening-test-04/",

                "https://ieltstrainingonline.com/audio-script-cambridge-ielts-13-listening-test-01/",
                "https://ieltstrainingonline.com/audio-script-cambridge-ielts-13-listening-test-02/",
                "https://ieltstrainingonline.com/audio-script-cambridge-ielts-13-listening-test-03/",
                "https://ieltstrainingonline.com/audio-script-cambridge-ielts-13-listening-test-04/",

                "https://ieltstrainingonline.com/audio-script-cambridge-ielts-12-listening-test-01/",
                "https://ieltstrainingonline.com/audio-script-cambridge-ielts-12-listening-test-02/",
                "https://ieltstrainingonline.com/audio-script-cambridge-ielts-12-listening-test-03/",
                "https://ieltstrainingonline.com/audio-script-cambridge-ielts-12-listening-test-04/",

                "https://ieltstrainingonline.com/audio-script-cambridge-ielts-11-listening-test-01/",
                "https://ieltstrainingonline.com/audio-script-cambridge-ielts-11-listening-test-02/",
                "https://ieltstrainingonline.com/audio-script-cambridge-ielts-11-listening-test-03/",
                "https://ieltstrainingonline.com/audio-script-cambridge-ielts-11-listening-test-04/",

                "https://ieltstrainingonline.com/audio-script-cambridge-ielts-10-listening-test-01/",
                "https://ieltstrainingonline.com/audio-script-cambridge-ielts-10-listening-test-02/",
                "https://ieltstrainingonline.com/audio-script-cambridge-ielts-10-listening-test-03/",
                "https://ieltstrainingonline.com/audio-script-cambridge-ielts-10-listening-test-04/"
        );

        Set<String> stopWords = FileUtils.readLines(new File("/Users/zhangjike/code/GistRepo/spider/src/main/java/stopwords.txt"), Charset.defaultCharset())
                .stream()
                .map(s -> s.replaceAll("[^a-zA-Z ]", "").toLowerCase().split("\\s+"))
                .flatMap(Arrays::stream)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());

        Map<String, Long> wordCntMap = Maps.newHashMap();

        for (String pageUrl : pageUrlList) {
            Document document = Jsoup.connect(pageUrl).get();
            String text = document.body().text();
            String[] wordTokens = text.replaceAll("[^a-zA-Z ]", "").toLowerCase().split("\\s+");
            for (int i = 0; i < wordTokens.length; i++) {
                String wordToken = wordTokens[i];
                if (stopWords.contains(wordToken)) {
                    continue;
                }
                wordCntMap.putIfAbsent(wordToken, 0L);
                wordCntMap.computeIfPresent(wordToken, (word, oldValue) -> oldValue+1);
            }
        }
        List<Pair<String, Long>> collect = wordCntMap.entrySet()
                .stream()
                .map(stringLongEntry -> Pair.of(stringLongEntry.getKey(), stringLongEntry.getValue()))
                .sorted(Comparator.comparing(Pair<String, Long>::getRight).reversed())
                .collect(Collectors.toList());
        System.out.println("have a nice day");
    }

}
