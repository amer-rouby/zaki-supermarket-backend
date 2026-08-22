package com.zakisupermarket.service.impl;

import com.zakisupermarket.dto.response.EInvoiceSubmissionResponse;
import com.zakisupermarket.entity.EInvoiceSubmission;
import com.zakisupermarket.entity.SaleTransaction;
import com.zakisupermarket.exception.FeatureDisabledException;
import com.zakisupermarket.repository.EInvoiceSubmissionRepository;
import com.zakisupermarket.repository.SaleTransactionRepository;
import com.zakisupermarket.service.EInvoiceService;
import com.zakisupermarket.service.EtaIntegrationService;
import com.zakisupermarket.service.EtaSubmissionResult;
import com.zakisupermarket.service.settings.ZakiFeatureSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EInvoiceServiceImpl implements EInvoiceService {

    private final EInvoiceSubmissionRepository eInvoiceSubmissionRepository;
    private final SaleTransactionRepository saleTransactionRepository;
    private final EtaIntegrationService etaIntegrationService;
    private final ZakiFeatureSettingsService zakiFeatureSettingsService;

    @Override
    @Transactional
    public EInvoiceSubmissionResponse submit(Long saleId, Long storeId) {
        checkEnabled(storeId);
        SaleTransaction sale = findSale(saleId, storeId);

        EInvoiceSubmission submission = eInvoiceSubmissionRepository.findBySaleTransactionId(saleId)
                .orElseGet(() -> EInvoiceSubmission.builder().saleTransaction(sale).build());

        attemptSubmission(submission);
        return EInvoiceSubmissionResponse.fromEntity(eInvoiceSubmissionRepository.save(submission));
    }

    @Override
    @Transactional
    public EInvoiceSubmissionResponse retry(Long saleId, Long storeId) {
        checkEnabled(storeId);
        findSale(saleId, storeId);

        EInvoiceSubmission submission = eInvoiceSubmissionRepository.findBySaleTransactionId(saleId)
                .orElseThrow(() -> new RuntimeException("No e-invoice submission exists yet for this sale"));

        submission.setRetryCount(submission.getRetryCount() + 1);
        attemptSubmission(submission);
        return EInvoiceSubmissionResponse.fromEntity(eInvoiceSubmissionRepository.save(submission));
    }

    @Override
    @Transactional(readOnly = true)
    public EInvoiceSubmissionResponse getForSale(Long saleId, Long storeId) {
        checkEnabled(storeId);
        findSale(saleId, storeId);
        return eInvoiceSubmissionRepository.findBySaleTransactionId(saleId)
                .map(EInvoiceSubmissionResponse::fromEntity)
                .orElse(null);
    }

    private SaleTransaction findSale(Long saleId, Long storeId) {
        return saleTransactionRepository.findByIdAndStoreId(saleId, storeId)
                .orElseThrow(() -> new RuntimeException("Sale not found"));
    }

    private void attemptSubmission(EInvoiceSubmission submission) {
        EtaSubmissionResult result = etaIntegrationService.submit(submission.getSaleTransaction());
        submission.setSubmittedAt(LocalDateTime.now());
        if (result.success()) {
            submission.setStatus(EInvoiceSubmission.Status.SUBMITTED);
            submission.setEtaUuid(result.etaUuid());
            submission.setErrorMessage(null);
        } else {
            submission.setStatus(EInvoiceSubmission.Status.ERROR);
            submission.setErrorMessage(result.errorMessage());
        }
    }

    private void checkEnabled(Long storeId) {
        Boolean enabled = zakiFeatureSettingsService.getOrCreate(storeId).getEInvoiceEnabled();
        if (enabled != null && !enabled) {
            throw new FeatureDisabledException("FEATURE_DISABLED_EINVOICE", "E-invoice (ETA) feature is disabled for this store");
        }
    }
}
