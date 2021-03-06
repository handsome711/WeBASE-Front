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

import com.webank.webase.front.base.exception.FrontException;
import com.webank.webase.front.base.properties.Constants;
import com.webank.webase.front.performance.result.PerformanceData;
import com.webank.webase.front.processperformance.entity.ProcessPerformance;
import com.webank.webase.front.performance.result.Data;
import com.webank.webase.front.performance.result.LineDataList;
import lombok.extern.slf4j.Slf4j;
import org.hyperic.sigar.*;
import org.hyperic.sigar.cmd.Ps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Host monitor: monitor computer's process performance
 * such as cpu, memory, disk etc.
 */

@Slf4j
@Service
public class ProcessPerformanceService {

    @Autowired
    private ProcessPerformanceRepository processPerformanceRepository;
    @Autowired
    private Constants constants;
    // host upload bps(bit per second)
    private static  final String TXBPS = "txbps";
    // host download bps(bit per second)
    private static final String RXBPS = "rxbps";

    private static Sigar sigar = new Sigar();

    /**
     * findByTime.
     * 
     * @param startTime startTime
     * @param endTime endTime
     * @return
     */
    public List<ProcessPerformance> findByTime(LocalDateTime startTime, LocalDateTime endTime) {
        return processPerformanceRepository.findByTimeBetween(
                startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }

    /**
     * findProcessContrastDataByTime.
     *
     * @param startTime startTime
     * @param endTime endTime
     * @param contrastStartTime contrastStartTime
     * @param contrastEndTime contrastEndTime
     * @param gap gap
     * @return
     */
    public List<PerformanceData> findProcessContrastDataByTime(LocalDateTime startTime,
                                                               LocalDateTime endTime, LocalDateTime contrastStartTime, LocalDateTime contrastEndTime,
                                                               int gap)  {

        List<ProcessPerformance> performanceList;
        if (startTime == null || endTime == null) {
            performanceList = new ArrayList<>();
        } else {
            performanceList = processPerformanceRepository.findByTimeBetween(
                    startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        List<ProcessPerformance> contrastProcessPerformanceList = new ArrayList<>();
        if (contrastStartTime != null && contrastEndTime != null) {
            contrastProcessPerformanceList = processPerformanceRepository.findByTimeBetween(
                    contrastStartTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    contrastEndTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        return transferToPerformanceData(transferListByGap(performanceList, gap),
                transferListByGap(contrastProcessPerformanceList, gap));
    }

    private List<PerformanceData> transferToPerformanceData(List<ProcessPerformance> performanceList,
                                                            List<ProcessPerformance> contrastProcessPerformanceList) {
        List<Long> timestampList = new ArrayList<>();
        List<BigDecimal> memoryValueList = new ArrayList<>();
        List<BigDecimal> cpuValueList = new ArrayList<>();
//        List<BigDecimal> diskValueList = new ArrayList<>();
//        List<BigDecimal> rxbpsValueList = new ArrayList<>();
//        List<BigDecimal> txbpsValueList = new ArrayList<>();
        for (ProcessPerformance processPerformance : performanceList) {
            cpuValueList.add(processPerformance.getCpuUseRatio());
            memoryValueList.add(processPerformance.getMemoryUseRatio());
//            diskValueList.add(processPerformance.getDiskUseRatio());
            timestampList.add(processPerformance.getTimestamp());
//            rxbpsValueList.add(processPerformance.getRxbps());
//            txbpsValueList.add(processPerformance.getTxbps());
        }
        performanceList.clear();

        List<Long> contrastTimestampList = new ArrayList<>();
        List<BigDecimal> contrastMemoryValueList = new ArrayList<>();
        List<BigDecimal> contrastCpuValueList = new ArrayList<>();
//        List<BigDecimal> contrastDiskValueList = new ArrayList<>();
//        List<BigDecimal> contrastRxbpsValueList = new ArrayList<>();
//        List<BigDecimal> contrastTxbpsValueList = new ArrayList<>();
        for (ProcessPerformance processPerformance : contrastProcessPerformanceList) {
            contrastCpuValueList.add(processPerformance.getCpuUseRatio());
            contrastMemoryValueList.add(processPerformance.getMemoryUseRatio());
//            contrastDiskValueList.add(processPerformance.getDiskUseRatio());
//            contrastRxbpsValueList.add(processPerformance.getRxbps());
//            contrastTxbpsValueList.add(processPerformance.getTxbps());
            contrastTimestampList.add(processPerformance.getTimestamp());
        }
        contrastProcessPerformanceList.clear();
        List<PerformanceData> performanceDataList = new ArrayList<>();
        performanceDataList.add(
                new PerformanceData("cpu", new Data(new LineDataList(timestampList, cpuValueList),
                        new LineDataList(contrastTimestampList, contrastCpuValueList))));
        performanceDataList
                .add(new PerformanceData("memory", new Data(new LineDataList(null, memoryValueList),
                        new LineDataList(null, contrastMemoryValueList))));
//        performanceDataList
//                .add(new PerformanceData("disk", new Data(new LineDataList(null, diskValueList),
//                        new LineDataList(null, contrastDiskValueList))));
//        performanceDataList
//                .add(new PerformanceData(TXBPS, new Data(new LineDataList(null, txbpsValueList),
//                        new LineDataList(null, contrastTxbpsValueList))));
//        performanceDataList
//                .add(new PerformanceData(RXBPS, new Data(new LineDataList(null, rxbpsValueList),
//                        new LineDataList(null, contrastRxbpsValueList))));
        return performanceDataList;
    }

    public boolean toggleSync(boolean toggle) throws Exception {
        constants.setMonitorEnabled(toggle);
        if(constants.isMonitorEnabled() == toggle) {
            log.debug("toggle sync process performance status to " + toggle);
            return toggle;
        }else {
            throw new FrontException("Fail to toggle sync process performance status to "+ toggle);
        }
    }

    public boolean getToggleStatus() throws Exception {
        return constants.isMonitorEnabled();
    }

    /**
     * syncProcessPerformanceInfo per 5s
     */
    @Scheduled(cron = "0/5 * * * * ?")
    public void syncProcessPerformanceInfo() throws SigarException {
        log.debug("begin sync process performance");
        if (!constants.isMonitorEnabled())
        {
            return;
        }
        ProcessPerformance processProcessPerformance = new ProcessPerformance();

        Ps ps = new Ps();
        try {
            long[] pids = sigar.getProcList();
            for(long pid : pids){
                List<String> list = ps.getInfo(sigar, pid);
                String[] splitProcessName = list.get(8).split("/");
                if (splitProcessName[splitProcessName.length - 1].equals("fisco-bcos")){
                    ProcCpu procCpu = sigar.getProcCpu(String.valueOf(pid));
                    processProcessPerformance.setCpuUseRatio(BigDecimal.valueOf(procCpu.getPercent()));
                    ProcMem procMem = sigar.getProcMem(pid);
                    processProcessPerformance.setCpuUseRatio(BigDecimal.valueOf(procMem.getSize() / sigar.getMem().getTotal()));
                    Long currentTime = System.currentTimeMillis();
                    processProcessPerformance.setTimestamp(currentTime);
                    processPerformanceRepository.save(processProcessPerformance);
                    log.debug("insert success =  " + processProcessPerformance.getId());
                    return;
                }
//                    case 0 : info.setPid(list.get(0)); break;
//                    case 1 : info.setUser(list.get(1)); break;
//                    case 2 : info.setStartTime(list.get(2)); break;
//                    case 3 : info.setMemSize(list.get(3)); break;
//                    case 4 : info.setMemUse(list.get(4)); break;
//                    case 5 : info.setMemhare(list.get(5)); break;
//                    case 6 : info.setState(list.get(6)); break;
//                    case 7 : info.setCpuTime(list.get(7)); break;
//                    case 8 : info.setName(list.get(8)); break;
            }
            log.debug("insert error, can not find process");
            return;
        } catch (SigarException e) {
            e.printStackTrace();
        }
    }



//    private BigDecimal getCpuRatio() throws SigarException {
//        CpuPerc cpuPerc = sigar.getCpuPerc();
//        return  BigDecimal.valueOf(cpuPerc.getCombined() * 100);
//
//    }
//
//    private BigDecimal getMemoryRatio() throws SigarException {
//        ;
//        Mem mem = sigar.getMem();
//        // log.info("内存总量: " + mem.getTotal() / 1024L + "K av");
//        return  BigDecimal.valueOf(mem.getUsedPercent());
//    }

    /**
     * getDiskRatio.
     * 
     * @return
     */
//    public BigDecimal getDiskRatio() throws SigarException {
//        double use;
//        use = sigar.getFileSystemUsage(constants.getMonitorDisk()).getUsePercent();
//        return  BigDecimal.valueOf(use * 100); // 硬盘使用百分率%
//    }

    /**
     * getNetSpeed.
     * 
     * @return
     */
//    public Map<String, Long> getNetSpeed()
//            throws UnknownHostException, SigarException, InterruptedException {
//        Map<String, Long> map = new HashMap<String, Long>();
//        InetAddress addr;
//        String ip;
//        try {
//            addr = InetAddress.getLocalHost();
//             ip = addr.getHostAddress();
//        } catch (Exception e ) {
//            log.info("sigar get ip failed!");
//            ip = "127.0.0.1";
//        }
//
//        String[] ifNames = sigar.getNetInterfaceList();
//        long rxbps = 0;
//        long txbps = 0;
//        for (int i = 0; i < ifNames.length; i++) {
//            String name = ifNames[i];
//            NetInterfaceConfig ifconfig = sigar.getNetInterfaceConfig(name);
//            if (ifconfig.getAddress().equals(ip)) {
//                long start = System.currentTimeMillis();
//                NetInterfaceStat statStart = sigar.getNetInterfaceStat(name);
//                long rxBytesStart = statStart.getRxBytes();
//                long txBytesStart = statStart.getTxBytes();
//                Thread.sleep(1000);
//                long end = System.currentTimeMillis();
//                NetInterfaceStat statEnd = sigar.getNetInterfaceStat(name);
//                long rxBytesEnd = statEnd.getRxBytes();
//                long txBytesEnd = statEnd.getTxBytes();
//
//                rxbps = ((rxBytesEnd - rxBytesStart) * 8 / (end - start) * 1000) / 1024 / 8;
//                txbps = ((txBytesEnd - txBytesStart) * 8 / (end - start) * 1000) / 1024 / 8;
//                break;
//            }
//        }
//        map.put(RXBPS, rxbps);
//        map.put(TXBPS, txbps);
//        return map;
//    }

    /**
     * getConfigInfo.
     * 
     * @return
     */
//    public Map<String, String> getConfigInfo() throws UnknownHostException, SigarException {
//        Map<String, String> configMap = new HashMap<>();
//        InetAddress addr;
//        addr = InetAddress.getLocalHost();
//        String ip = addr.getHostAddress();
//        log.info("本地ip地址:    " + ip);
//        configMap.put("ip", ip);
//        Mem mem = sigar.getMem();
//        log.info("内存总量:    " + mem.getTotal() / 1024L + "K av");
//        log.info("当前内存使用量:    " + mem.getUsed() / 1024L + "K used");
//        configMap.put("memoryTotalSize", Long.toString(mem.getTotal() / 1024L));
//        configMap.put("memoryUsedSize", Long.toString(mem.getUsed() / 1024L));
//        CpuPerc cpu = sigar.getCpuPerc();
//        CpuInfo[] infos = sigar.getCpuInfoList();
//        log.info("CPU的大小:    " + infos[0].getMhz());
//        log.info("CPU的核数:    " + infos.length);
//        configMap.put("cpuSize", Integer.toString(infos[0].getMhz()));
//        configMap.put("cpuAmount", Integer.toString(infos.length));
//        long total;
//        long use;
//        FileSystem[] fslist = sigar.getFileSystemList();
//        log.info("****fs " + fslist.length);
//        use = sigar.getFileSystemUsage(constants.getMonitorDisk()).getUsed();
//        total = sigar.getFileSystemUsage(constants.getMonitorDisk()).getTotal();
//        log.info("文件系统总量:    " + total);
//        log.info("文件系统已使用量:    " + use);
//        configMap.put("diskTotalSize", Long.toString(total));
//        configMap.put("diskUsedSize", Long.toString(use));
//        return configMap;
//    }

    /**
     * transferListByGap.
     * 
     * @param arrayList arrayList
     * @param gap gap
     * @return
     */
    public List transferListByGap(List arrayList, int gap)  {
        if (gap == 0) {
             throw new FrontException("gap cannot be 0");
        }
        List newProcessPerformanceList = fillList(arrayList);
        List ilist = new ArrayList<>();
        int len = newProcessPerformanceList.size();
        for (int i = 0; i < len; i = i + gap) {
            ilist.add(newProcessPerformanceList.get(i));
        }
        return ilist;
    }

    private List<ProcessPerformance> fillList(List<ProcessPerformance> performanceList) {
        List<ProcessPerformance> newProcessPerformanceList = new ArrayList<>();
        for (int i = 0; i < performanceList.size() - 1; i++) {
            Long startTime = performanceList.get(i).getTimestamp();
            Long endTime = performanceList.get(i + 1).getTimestamp();
            if (endTime - startTime > 10000) {
                log.info("****startTime" + startTime);
                log.info("****endTime" + endTime);
                while (endTime - startTime > 5000) {
                    ProcessPerformance emptyProcessPerformance = new ProcessPerformance();
                    emptyProcessPerformance.setTimestamp(startTime + 5000);
                    newProcessPerformanceList.add(emptyProcessPerformance);
                    log.info("****insert" + startTime);
                    startTime = startTime + 5000;
                }
            } else {
                newProcessPerformanceList.add(performanceList.get(i));
            }
        }
        return newProcessPerformanceList;
    }
    /**
     * deleteProcessPerformanceInfoPerWeek at 00:00:00 per week
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void deleteProcessPerformanceInfoPerWeek() throws SigarException {
        log.debug("begin delete process performance");
        if (!constants.isMonitorEnabled())
        {
            return;
        }
        Long currentTime = System.currentTimeMillis();
        Long weekAgo = currentTime - 3600 * 24 * 7 * 1000;
        int i = processPerformanceRepository.deleteTimeAgo(weekAgo);
        log.debug("delete record count = " + i);
    }

}


