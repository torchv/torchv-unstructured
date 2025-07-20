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


package com.torchv.infra.unstructured.parser.word.analyzer;

import java.util.List;

/**
 * 内容验证结果
 * 
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * @since torchv_server
 */
public class TableValidationResult {
    
    private final List<String> foundTexts;
    private final List<String> missingTexts;
    
    public TableValidationResult(List<String> foundTexts, List<String> missingTexts) {
        this.foundTexts = foundTexts;
        this.missingTexts = missingTexts;
    }
    
    public List<String> getFoundTexts() {
        return foundTexts;
    }
    
    public List<String> getMissingTexts() {
        return missingTexts;
    }
    
    public int getFoundCount() {
        return foundTexts.size();
    }
    
    public int getMissingCount() {
        return missingTexts.size();
    }
    
    public int getTotalCount() {
        return foundTexts.size() + missingTexts.size();
    }
    
    public boolean isAllFound() {
        return missingTexts.isEmpty();
    }
    
    public double getSuccessRate() {
        if (getTotalCount() == 0) {
            return 1.0;
        }
        return (double) getFoundCount() / getTotalCount();
    }
    
    @Override
    public String toString() {
        return String.format("ValidationResult{found=%d, missing=%d, successRate=%.2f}",
                getFoundCount(), getMissingCount(), getSuccessRate());
    }
}
