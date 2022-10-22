package nttdata.bootcamp.mscreditstransactions.interfaces;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import nttdata.bootcamp.mscreditstransactions.config.CreditTransationRestConfig;
import nttdata.bootcamp.mscreditstransactions.dto.CreditDTO;

@Service
public class CreditServiceImpl implements ICreditService {
    @Autowired
    private CreditTransationRestConfig creditRest;

    @Override
    public CreditDTO findCreditById(String id) {
        Map<String, String> param = new HashMap<String, String>();
        param.put("id", id);
        String uri = "http://localhost:8090/api/credits/{id}";
        final CreditDTO dto = creditRest.getForObject(uri, CreditDTO.class, param);
        return dto;
    }

    @Override
    public ResponseEntity<?> updateCredit(CreditDTO dto) {
        HttpEntity<CreditDTO> body = new HttpEntity<CreditDTO>(dto);
        String uri = "http://localhost:8090/api/credits";
        ResponseEntity<CreditDTO> response = creditRest.exchange(
                uri,
                HttpMethod.PUT,
                body,
                CreditDTO.class);
        return response;
    }

    @Override
    public Optional<CreditDTO> findCreditByNroCredit(String nroCredit) {
        Map<String, String> param = new HashMap<String, String>();
        param.put("nroCredit", nroCredit);
        String uri = "http://localhost:8090/api/credits/byNroCredit/{nroCredit}";
        final CreditDTO dto = creditRest.getForObject(uri, CreditDTO.class, param);
        Optional<CreditDTO> result=Optional.ofNullable(dto); 
        return result;
    }

}
