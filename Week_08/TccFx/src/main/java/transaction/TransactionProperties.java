package transaction;

import transaction.TransactionBuilder;

public interface TransactionProperties {
    TransactionBuilder<?> initializeTransactionBuilder();

    String getTxType();
}
