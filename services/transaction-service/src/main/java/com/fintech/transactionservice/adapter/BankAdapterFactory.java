package com.fintech.transactionservice.adapter;

import com.fintech.transactionservice.annotation.BankCode;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BankAdapterFactory {
    private final Map<String, BankAdapter> adapters = new HashMap<>();

    public BankAdapterFactory(List<BankAdapter> adapterList) {
        for (BankAdapter adapter : adapterList) {
            BankCode annotation = adapter.getClass().getAnnotation(BankCode.class);
            if (annotation != null) {
                adapters.put(annotation.value(), adapter);
            }
        }
    }


    public BankAdapter getAdapter(String bankCode) {
        return adapters.get(bankCode);
    }
}
