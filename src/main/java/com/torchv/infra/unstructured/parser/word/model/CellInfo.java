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


package com.torchv.infra.unstructured.parser.word.model;

/**
 * 单元格信息
 * 
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * @since torchv_server
 */
public class CellInfo {
    
    private final String text;
    private final int colspan;
    private final int rowspan;
    
    public CellInfo(String text, int colspan, int rowspan) {
        this.text = text;
        this.colspan = colspan;
        this.rowspan = rowspan;
    }
    
    public String getText() {
        return text;
    }
    
    public int getColspan() {
        return colspan;
    }
    
    public int getRowspan() {
        return rowspan;
    }
    
    @Override
    public String toString() {
        return String.format("CellInfo{text='%s', colspan=%d, rowspan=%d}", text, colspan, rowspan);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        
        CellInfo cellInfo = (CellInfo) o;
        return colspan == cellInfo.colspan &&
                rowspan == cellInfo.rowspan &&
                (text != null ? text.equals(cellInfo.text) : cellInfo.text == null);
    }
    
    @Override
    public int hashCode() {
        int result = text != null ? text.hashCode() : 0;
        result = 31 * result + colspan;
        result = 31 * result + rowspan;
        return result;
    }
}
