package com.basetonedata.ftd.mybatis.service.impl;

import com.basetonedata.ftd.mybatis.service.EncAndDecryptService;
import com.basetonedata.ftd.mybatis.util.AesUtil;
import com.google.common.base.Charsets;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by lucas on 2020/7/9.
 */
@Slf4j
public class DefaultAesServiceImpl implements EncAndDecryptService {
    private String aesKey = "14LbKUwKowbSgARK6/uSpw==";

    public DefaultAesServiceImpl(String aesKey) {
        this.aesKey = aesKey;
    }

    public DefaultAesServiceImpl() {
    }

    @Override
    public String decrypt(String secretStr) {
        try {
            return AesUtil.decrypt(secretStr, aesKey, Charsets.UTF_8.toString());
        } catch (Exception e) {
            log.error("decrypt error", e);
            return null;
        }
    }

    @Override
    public String encrypt(String originStr) {
        try {
            return AesUtil.encrypt(originStr, aesKey, Charsets.UTF_8.toString());
        } catch (Exception e) {
            log.error("encrypt error", e);
            return null;
        }
    }
}
