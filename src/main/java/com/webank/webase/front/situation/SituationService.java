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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.webank.webase.front.base.exception.FrontException;
import com.webank.webase.front.performance.result.Data;
import com.webank.webase.front.performance.result.LineDataList;
import com.webank.webase.front.performance.result.PerformanceData;
import com.webank.webase.front.situation.entity.Situation;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.fisco.bcos.web3j.protocol.Web3j;
import org.fisco.bcos.web3j.protocol.core.methods.response.PendingTxSize;
import org.fisco.bcos.web3j.protocol.core.methods.response.TotalTransactionCount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
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
        List<Situation> situations = situationRepository.findSituationDataNow(groupId);
//        return (situations != null)? situations.get(situations.size() - 1): new Situation();
        return (situations != null)? situations.get(0): new Situation();
    }

    public List<Situation> findSituationDataNow2(int groupId) {
        return situationRepository.findSituationDataNow(groupId);
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
//        situation.setTxPool((int)(Math.random()*101));
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
        log.debug("begin sync situation data");

        Long currentTime = System.currentTimeMillis();
        //to do  add  more group
        for(Map.Entry<Integer,Web3j> entry : web3jMap.entrySet()) {
            Situation situation = new Situation();

            randValue(situation);
            CompletableFuture<PendingTxSize> pendingTxSizeFuture = entry.getValue().getPendingTxSize().sendAsync();
            situation.setTxPool(pendingTxSizeFuture.get().getPendingTxSize());
            situation.setTimestamp(currentTime);
            situation.setGroupId(entry.getKey());
            situationRepository.save(situation);
            log.debug("insert success =  " + situation.getId());
        }
    }

    /**
     * scheduled task to delete Situation Info at 00:00:00 per day
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void deleteSituationInfoPerWeek()   {
        log.debug("begin delete situation");
        Long currentTime = System.currentTimeMillis();
        Long aDayAgo = currentTime - 3600 * 24 * 1000;
        int i = situationRepository.deleteTimeAgo(aDayAgo);
        log.debug("delete record count = " + i);
    }
    public String[] test() {
        JSONObject json = new JSONObject();
        //        "jsonrpc":"2.0","method":"getBlockVerifierStatus","params":[1],"id":1
        json.put("jsonrpc", "2.0");
        json.put("method","getBlockVerifierStatus");
        int[] arr = {1};
        json.put("params", arr);
        json.put("id",1);

        String URL = "http://127.0.0.1:8545";
        return sendPost(json, URL);
    }

    public static String[] sendPost(JSONObject json, String URL) {

        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(URL);
        post.setHeader("Content-Type", "application/json");
        post.addHeader("Authorization", "Basic YWRtaW46");
        String result = "";
        String a [] = new String[10];
        a[0] = json.toString();
        a[1] = URL;
        try {
            StringEntity s = new StringEntity(json.toString(), "utf-8");
            s.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE,
                    "application/json"));
            post.setEntity(s);
            // 发送请求
            HttpResponse httpResponse = client.execute(post);

            // 获取响应输入流
            InputStream inStream = httpResponse.getEntity().getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    inStream, "utf-8"));
//            StringBuilder strber = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null)
                result += line;
//                strber.append(line + "\n");
            inStream.close();

//            result = strber.toString();
            System.out.println(result);
            a[2] = result;

            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                System.out.println("请求服务器成功，做相应处理");

            } else {
                System.out.println("请求服务端失败");
            }


        } catch (Exception e) {
            System.out.println("请求异常");
            throw new RuntimeException(e);
        }
        JSONObject testJson = JSON.parseObject(result);
        a[3] = testJson.toString();
        a[4] = testJson.getString("result");
        JSONObject testJson2 = JSON.parseObject(testJson.getString("result"));
        a[5] = testJson2.toString();
        a[6] = testJson2.getString("executingNumber");
        a[7] = testJson2.getString("isExecuting");

        Integer i1 = Integer.valueOf(testJson2.getString("executingNumber"));
        Integer i2 = Integer.valueOf(testJson2.getString("isExecuting"));

        a[8] = i1.toString();
        a[9] = i2.toString();
        return a;
    }
}
