package nttdata.bootcamp.mscreditstransactions.interfaces;

import java.util.Optional;

import org.springframework.http.ResponseEntity;

import nttdata.bootcamp.mscreditstransactions.dto.CreditDTO;

public interface ICreditService {
    public CreditDTO findCreditById(String id);

    public Optional<CreditDTO> findCreditByNroCredit(String nroCredit);

    public ResponseEntity<?> updateCredit(CreditDTO dto);
}
