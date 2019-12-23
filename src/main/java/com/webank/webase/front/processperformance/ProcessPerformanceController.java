/*
 * Copyright 2014-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.webank.webase.front.processperformance;

import com.webank.webase.front.performance.result.PerformanceData;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

/**
 * Host monitor controller
 * monitor of host computer's performance
 * such as cpu, memory, disk etc.
 */

@RestController
@RequestMapping(value = "/process")
public class ProcessPerformanceController {

    @Autowired
    private ProcessPerformanceService processPerformanceService;

    /**
     * query process performance data.
     * 
     * @param beginDate beginDate
     * @param endDate endDate
     * @param contrastBeginDate contrastBeginDate
     * @param contrastEndDate contrastEndDate
     * @param gap gap
     * @return
     */
    @ApiOperation(value = "query process performance data", notes = "query process performance data")
    @ApiImplicitParams({@ApiImplicitParam(name = "beginDate", value = "start time"),
            @ApiImplicitParam(name = "endDate", value = "end time"),
            @ApiImplicitParam(name = "contrastBeginDate", value = "compare start time"),
            @ApiImplicitParam(name = "contrastEndDate", value = "compare end time"),
            @ApiImplicitParam(name = "gap", value = "time gap", dataType = "int")})
    @GetMapping
    public List<PerformanceData> getPerformanceRatio(
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DATE_TIME) LocalDateTime beginDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DATE_TIME) LocalDateTime contrastBeginDate,
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DATE_TIME) LocalDateTime contrastEndDate,
            @RequestParam(required = false, defaultValue = "1") int gap) throws Exception {
        List<PerformanceData> performanceList = processPerformanceService.findProcessContrastDataByTime(beginDate,
                endDate, contrastBeginDate, contrastEndDate, gap);
        return performanceList;
    }
}
