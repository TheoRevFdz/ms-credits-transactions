package nttdata.bootcamp.mscreditstransactions.interfaces;

import nttdata.bootcamp.mscreditstransactions.dto.CreditDTO;

public interface ICreditService {
    public CreditDTO findCreditById(String id);
}
