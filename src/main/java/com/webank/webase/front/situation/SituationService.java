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
package com.webank.webase.front.situation;

import com.webank.webase.front.base.exception.FrontException;
import com.webank.webase.front.performance.result.Data;
import com.webank.webase.front.performance.result.LineDataList;
import com.webank.webase.front.performance.result.PerformanceData;
import com.webank.webase.front.situation.entity.Situation;
import lombok.extern.slf4j.Slf4j;
import org.fisco.bcos.web3j.protocol.Web3j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Node situation service
 * distinguished from host situation: performance
 */

@Slf4j
@Service
public class SituationService {
    @Autowired
    Map<Integer,Web3j> web3jMap;
    @Autowired
    SituationRepository situationRepository;

    public List<PerformanceData> findSituationDataByTime(int groupId, LocalDateTime startTime, LocalDateTime endTime, int gap)  {

        List<Situation> situationList;
        if (startTime == null || endTime == null) {
            situationList = new ArrayList<>();
        } else {
            situationList = situationRepository.findByTimeBetween(groupId,startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        return transferToPerformanceData(transferListByGap(situationList, gap));

    }

    public Situation findSituationDataNow(int groupId) {
        Situation situation = situationRepository.findSituationDataNow(groupId);
        return situation;
    }

    private List<PerformanceData> transferToPerformanceData(List<Situation> situationList) {
        List<Long> timestampList = new ArrayList<>();
        List<BigDecimal> txPoolList = new ArrayList<>();
        List<BigDecimal> sealerList = new ArrayList<>();
        List<BigDecimal> consensusEngineBlockList = new ArrayList<>();
        List<BigDecimal> consensusEngineCommonViewList = new ArrayList<>();
        List<BigDecimal> blockChainList = new ArrayList<>();
        List<BigDecimal> blockVerifierList = new ArrayList<>();
        for (Situation situation : situationList) {
            txPoolList.add(new BigDecimal(situation.getTxPool()));
            sealerList.add(new BigDecimal(situation.getSealer()));
            consensusEngineBlockList.add(new BigDecimal(situation.getConsensusEngineBlock()));
            consensusEngineCommonViewList.add(new BigDecimal(situation.getConsensusEngineCommonView()));
            blockChainList.add(new BigDecimal(situation.getBlockChain()));
            blockVerifierList.add(new BigDecimal(situation.getBlockVerifier()));
            
            timestampList.add(situation.getTimestamp());
        }
        situationList.clear();

        List<PerformanceData> performanceDataList = new ArrayList<>();
        performanceDataList.add(new PerformanceData("txPool", new Data(new LineDataList(timestampList, txPoolList), new LineDataList(timestampList, txPoolList))));
        performanceDataList.add(new PerformanceData("sealer", new Data(new LineDataList(null, sealerList), new LineDataList(null, sealerList))));
        performanceDataList.add(new PerformanceData("consensusEngineBlock", new Data(new LineDataList(null, consensusEngineBlockList), new LineDataList(null, consensusEngineBlockList))));
        performanceDataList.add(new PerformanceData("consensusEngineCommonView", new Data(new LineDataList(null, consensusEngineCommonViewList), new LineDataList(null, consensusEngineCommonViewList))));
        performanceDataList.add(new PerformanceData("blockChain", new Data(new LineDataList(null, blockChainList), new LineDataList(null, blockChainList))));
        performanceDataList.add(new PerformanceData("blockVerifier", new Data(new LineDataList(null, blockVerifierList), new LineDataList(null, blockVerifierList))));
        return performanceDataList;
    }

    public void randValue(Situation situation) {
        situation.setTxPool((int)(Math.random()*101));
        situation.setSealer((int)(Math.random()*101));
        situation.setConsensusEngineBlock((int)(Math.random()*101));
        situation.setConsensusEngineCommonView((int)(Math.random()*101));
        situation.setBlockChain((int)(Math.random()*101));
        situation.setBlockVerifier((int)(Math.random()*101));
    }

    public List transferListByGap(List arrayList, int gap)  {
        if (gap == 0) {
             throw new FrontException("gap cannot be 0");
        }
        List newSituationList= fillList(arrayList);
        List ilist = new ArrayList<>();
        int len = newSituationList.size();
        for (int i = 0; i < len; i = i + gap) {
            ilist.add(newSituationList.get(i));
        }
        return ilist;
    }

    private List<Situation> fillList(List<Situation> situationList) {
        List<Situation> newSituationList = new ArrayList<>();
        for (int i = 0; i < situationList.size() - 1; i++) {
            Long startTime = situationList.get(i).getTimestamp();
            Long endTime = situationList.get(i + 1).getTimestamp();
            if (endTime - startTime > 10000) {
                log.info("****startTime" + startTime);
                log.info("****endTime" + endTime);
                while (endTime - startTime > 5000) {
                    Situation emptySituation = new Situation();
                    emptySituation.setTimestamp(startTime + 5000);
                    newSituationList.add(emptySituation);
                    log.info("****insert" + startTime);
                    startTime = startTime + 5000;
                }
            } else {
                newSituationList.add(situationList.get(i));
            }
        }
        return newSituationList;
    }

    /**
     * scheduled task to sync Situation Info per 5s
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Scheduled(cron = "0/5 * * * * ?")
    public void syncSituationInfo() throws ExecutionException, InterruptedException {
        log.debug("begin sync chain data");

        Long currentTime = System.currentTimeMillis();
        //to do  add  more group
        for(Map.Entry<Integer,Web3j> entry : web3jMap.entrySet()) {
            Situation situation = new Situation();

            randValue(situation);

            situation.setTimestamp(currentTime);
            situation.setGroupId(entry.getKey());
            situationRepository.save(situation);
            log.debug("insert success =  " + situation.getId());
        }
    }

    /**
     * scheduled task to delete Situation Info at 00:00:00 per week
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void deleteSituationInfoPerWeek()   {
        log.debug("begin delete situation");
        Long currentTime = System.currentTimeMillis();
        Long aWeekAgo = currentTime - 3600 * 24 * 7 * 1000;
        int i = situationRepository.deleteTimeAgo(aWeekAgo);
        log.debug("delete record count = " + i);
    }
}
