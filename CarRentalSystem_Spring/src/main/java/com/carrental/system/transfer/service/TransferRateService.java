package com.carrental.system.transfer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.carrental.system.transfer.entity.TransferRate;
import com.carrental.system.transfer.repository.TransferRateRepository;

import java.util.List;
import java.util.Optional;

/**
 * 接送服務費率服務層 (Service)
 * 負責處理費率設定相關的商業邏輯與資料庫操作
 */
@Service
public class TransferRateService {

    @Autowired
    private TransferRateRepository transferRateRepository;

    /**
     * 取得所有接送費率設定
     */
    public List<TransferRate> findAll() {
        return transferRateRepository.findAll();
    }

    /**
     * 透過費率編號 (id) 查詢單筆費率設定
     */
    public TransferRate findById(Integer id) {
        Optional<TransferRate> optional = transferRateRepository.findById(id);
        return optional.orElse(null); // 如果有找到就回傳，沒找到回傳 null
    }

    /**
     * 儲存或更新一筆費率設定
     */
    public TransferRate save(TransferRate transferRate) {
        return transferRateRepository.save(transferRate);
    }

    /**
     * 刪除特定費率設定
     */
    public void deleteById(Integer id) {
        transferRateRepository.deleteById(id);
    }
}
