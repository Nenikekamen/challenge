package com.challenge.transaction.aplication.port.output;

import com.challenge.transaction.infrastructure.adapter.output.provider.dto.ProviderExecuteRequest;
import com.challenge.transaction.infrastructure.adapter.output.provider.dto.ProviderExecuteResponse;

public interface TransactionProviderPort {

    ProviderExecuteResponse execute(ProviderExecuteRequest request);
}
