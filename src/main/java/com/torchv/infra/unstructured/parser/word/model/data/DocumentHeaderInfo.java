/*
 * Copyright © 2025-2030 TorchV(xiaoymin@mengjia.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.torchv.infra.unstructured.parser.word.model.data;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 针对Doc格式的头部信息
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * 2024/5/5 15:08
 * @since torchv_server v1.7.3
 */
@Getter
@Setter
public class DocumentHeaderInfo {
    
    /**
     * Header信息正则表达式
     */
    public static final Pattern HEADER_CHINESE_PATTERN = Pattern.compile("标题_(\\d)", Pattern.CASE_INSENSITIVE);
    public static final Pattern HEADER_ENGLISH_PATTERN = Pattern.compile("Heading_(\\d)", Pattern.CASE_INSENSITIVE);
    
    /**
     * Header等级信息Map集合
     */
    private final Map<Integer, String> HEADER_LEVEL_MAP = Map.of(0, "", 1, "#", 2, "##", 3, "###", 4, "####", 5, "#####", 6, "######", 7, "#######", 8, "########", 9, "#########");
    /**
     * Header标签信息Map集合
     */
    private final static Map<String, String> HEADER_MAP = new HashMap<>();
    static {
        HEADER_MAP.put("h1", "#");
        HEADER_MAP.put("h2", "##");
        HEADER_MAP.put("h3", "###");
        HEADER_MAP.put("h4", "####");
        HEADER_MAP.put("h5", "#####");
        HEADER_MAP.put("h6", "######");
        HEADER_MAP.put("h7", "#######");
        HEADER_MAP.put("h8", "########");
        HEADER_MAP.put("h9", "#########");
    }
    /**
     * header等级
     */
    private int level = 0;
    /**
     * 是否为header
     */
    private boolean header;
    
    /**
     * HeaderText信息
     */
    private String headerText;
    
    /**
     * 初始化Header信息
     * @param tagName 标签信息，例如：h1,h2,h3,h4,h5,h6,h7,h8,h9
     */
    public void initByHeaderTag(String tagName) {
        if (containsHeader(tagName)) {
            // 包含Header信息
            setHeader(true);
            setHeaderText(headerText(tagName));
        }
    }
    /**
     * 获取header文本
     * @return header文本
     */
    public String headerLevelText() {
        return Objects.toString(HEADER_LEVEL_MAP.get(this.level), StrUtil.EMPTY);
    }
    
    /**
     * 获取Header文本
     * @param header header信息
     * @return header文本
     */
    public String headerText(String header) {
        return Objects.toString(HEADER_MAP.get(header), StrUtil.EMPTY);
    }
    
    /**
     * 是否包含header
     * @param header header信息
     * @return 是否包含
     */
    public boolean containsHeader(String header) {
        return HEADER_MAP.containsKey(header);
    }
    
    /**
     * 创建一个header信息
     * @param level header等级
     * @param header 是否为header
     * @return header信息
     */
    public static DocumentHeaderInfo of(int level, boolean header) {
        DocumentHeaderInfo headerInfo = new DocumentHeaderInfo();
        headerInfo.setLevel(level);
        headerInfo.setHeader(header);
        return headerInfo;
    }
    
    /**
     * 创建一个空的header信息
     * @return header信息
     */
    public static DocumentHeaderInfo empty() {
        DocumentHeaderInfo headerInfo = new DocumentHeaderInfo();
        headerInfo.setHeader(false);
        return headerInfo;
    }
    
}
