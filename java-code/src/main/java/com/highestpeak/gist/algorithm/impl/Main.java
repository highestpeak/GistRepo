package com.highestpeak.gist.algorithm.impl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 * @author zhangjike <zhangjike03@kuaishou.com>
 * Created on 2021-08-17
 * todo
 * http://koala.ink/posts/4ee58d50/
 * https://neil.fraser.name/writing/diff/
 * 另一种场景：我只需要判断是不是新增，如果新增就获取新增内容。如果是修改（删除和更新）就不用获取内容，立刻退出。对方使用全部的内容
 * 这些场景的变更需要对这几个步骤有很好的理解
 *
 * https://stackoverflow.com/questions/132478/how-to-perform-string-diffs-in-java
 *
 * https://github.com/google/diff-match-patch
 */
public class Main {
    public static void main(String[] args) {
        // String str1 = "xxx";
        // String str2 = "xxx";

        // 特殊需求：一旦判断是修改，则可以立刻退出
        // 特殊需求：有时候不需要取出修改的内容

        // 比较两个文本是否内容完全相等(==,equals O(n),md5比较)
        // str1.equals()

        // 公共前缀后缀
        // 判断两端极值
        // 二分查找判断公共前后缀
        // 去掉前后缀后如果有一个字符串为空，那么要么是新增要么是删除，这很好判断
        // 如果一旦判断是修改，则可以立刻退出那么在这一步就已经够了
        // guava库Strings.commonPrefix但是他这个没有使用二分法

        // 两次编辑

        // 差分算法：LCS或编辑距离或Myers算法
        // 某个长度内如果改动到达


        // https://mvnrepository.com/artifact/org.bitbucket.cowwoc/diff-match-patch/1.2
        GoogleDiffMatchPatch diffAlgorithm = new GoogleDiffMatchPatch();
        LinkedList<GoogleDiffMatchPatch.Diff> diff = diffAlgorithm.diff_main("Hello World.", "Goodbye World.");
        // Result: [(-1, "Hell"), (1, "G"), (0, "o"), (1, "odbye"), (0, " World.")]
        diffAlgorithm.diff_cleanupSemantic(diff);
        // Result: [(-1, "Hello"), (1, "Goodbye"), (0, " World.")]
        System.out.println(diff);

        diff = diffAlgorithm.diff_main("Hello World.", "new1 Hello new2 World. new3");
        // Result: [(-1, "Hell"), (1, "G"), (0, "o"), (1, "odbye"), (0, " World.")]
        diffAlgorithm.diff_cleanupSemantic(diff);
        // Result: [(-1, "Hello"), (1, "Goodbye"), (0, " World.")]
        System.out.println(diff);
    }

    private List<String> diffText(String preVersionText, String nextVersionText) {
        int nextLen = StringUtils.length(nextVersionText);
        int preLen = StringUtils.length(preVersionText);

        // 上一版本为空，下次直接送审所有内容. 避免计算下面的公共前后缀
        if (StringUtils.isBlank(preVersionText)) {
            return Collections.singletonList(nextVersionText);
        }

        // 判断长度，next长度小于pre时，必定发生了删除
        if (nextLen < preLen) {
            return Collections.singletonList(nextVersionText);
        }

        // 长度相同时，判断字符串完全相同，可快速判断是否发生修改
        if (nextLen == preLen) {
            return StringUtils.equals(preVersionText, nextVersionText) ? Collections.emptyList() : Collections.singletonList(nextVersionText);
        }

        // 两个版本文本去掉公共前后缀，如果只是新增一段连续内容，则去掉公共前后缀后原版本字符串必定为空
        String commonPrefix = Strings.commonPrefix(preVersionText, nextVersionText);
        int prefixLen = commonPrefix.length();
        // 可能出现前缀后段和后缀前段重合的情况，例如xxxxyy和xxxxyzzzyy，所以先截掉前缀
        String preVersionAfterRemovePrefix = preVersionText.substring(prefixLen);
        String nextVersionAfterRemovePrefix = nextVersionText.substring(prefixLen);
        String commonSuffix = Strings.commonSuffix(preVersionAfterRemovePrefix, nextVersionAfterRemovePrefix);
        int suffixLen = commonSuffix.length();
        String preVersionAfterRemoveCommon = preVersionAfterRemovePrefix.substring(0, preVersionAfterRemovePrefix.length() - suffixLen);
        String nextVersionAfterRemoveCommon = nextVersionAfterRemovePrefix.substring(0, nextVersionAfterRemovePrefix.length() - suffixLen);
        if (StringUtils.isBlank(preVersionAfterRemoveCommon)) {
            return Collections.singletonList(nextVersionAfterRemoveCommon);
        }

        // 最多判断两次新增
        // 如果仅仅新增两次，则next必定包含pre
        int indexOf = nextVersionAfterRemoveCommon.indexOf(preVersionAfterRemoveCommon);
        if (indexOf != -1) {
            return Lists.newArrayList(
                    nextVersionAfterRemoveCommon.substring(0, indexOf),
                    nextVersionAfterRemoveCommon.substring(indexOf + preVersionAfterRemoveCommon.length() - 1)
            );
        }

        // 上面是快速diff预处理
        // todo myers 算法找到所有新增，遇到修改就退出

        return Collections.singletonList(nextVersionText);
    }
}
