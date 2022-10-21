package nttdata.bootcamp.mscreditstransactions.interfaces;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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

}
