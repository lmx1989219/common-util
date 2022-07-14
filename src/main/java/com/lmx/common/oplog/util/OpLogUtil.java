package com.lmx.common.oplog.util;

import com.lmx.common.oplog.OpLog;
import com.lmx.common.oplog.OperationLog;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.List;

@Slf4j
public class OpLogUtil {
    /**
     * appender 格式只保留msg
     */
    private static Logger log_es = LoggerFactory.getLogger("es_log");

    public static void printLog(List l, OpLog opLogCollect) {
        OperationLog operationLog = new OperationLog();
        //偶数为改前值，奇数为改后值
        for (int i = 0; i < l.size(); ++i) {
            String oldVal = (String) l.get(i++);//0 2
            String newVal = (String) l.get(i++);//1 4
            operationLog.setModifyValue(newVal);
            operationLog.setOriginalValue(oldVal);
            //json结构输出，让filebeats完成后续导入kafka
            log_es.info(operationLog.toString());
        }
    }

    public static void printLog(List l) {
        log.info("modify data={}", l);
        OperationLog operationLog = new OperationLog();
        int i = 0;
        //偶数为改前值，奇数为改后值
        for (i = 2 * i; 2 * i < l.size() - 2; ++i) {
            String oldVal = (String) l.get(2 * i);
            String[] arr = oldVal.split("=");
            String newVal = (String) l.get(2 * i + 1);
            operationLog.setAppName(System.getProperty("spring.application.name"));
            operationLog.setModifyValue(newVal);
            operationLog.setOriginalValue(arr[1]);
            //json结构输出，让filebeats完成后续导入kafka
            operationLog.setTraceId(MDC.get("X-B3-TraceId"));
            operationLog.setTableName((String) l.get(l.size() - 2));
            operationLog.setTableId((String) l.get(l.size() - 1));
            operationLog.setField(arr[0]);
            log_es.info(operationLog.toString());
        }
    }
}
