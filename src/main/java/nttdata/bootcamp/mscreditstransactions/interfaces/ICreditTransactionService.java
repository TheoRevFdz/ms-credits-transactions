package nttdata.bootcamp.mscreditstransactions.interfaces;

import java.util.List;

import nttdata.bootcamp.mscreditstransactions.model.CreditTransaction;

public interface ICreditTransactionService {
    public CreditTransaction createTransaction(CreditTransaction ct);

    public CreditTransaction updateTransaction(CreditTransaction ct);

    public List<CreditTransaction> findTransactionByNroCreditAndType(String nroCredit, String type);
}
