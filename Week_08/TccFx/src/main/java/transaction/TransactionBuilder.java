package transaction;


public interface TransactionBuilder<T extends Transaction> {
    T build();
}
