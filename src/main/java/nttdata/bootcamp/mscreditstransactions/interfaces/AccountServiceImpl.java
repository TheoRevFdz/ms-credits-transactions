package nttdata.bootcamp.mscreditstransactions.interfaces;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import nttdata.bootcamp.mscreditstransactions.config.RestConfig;
import nttdata.bootcamp.mscreditstransactions.dto.AccountDTO;
import nttdata.bootcamp.mscreditstransactions.dto.AccountTransactionDTO;

@Service
public class AccountServiceImpl implements IAccountService {
    @Autowired
    private RestConfig rest;

    @Override
    public Optional<AccountDTO> findAccountByNro(String nroAccount) {
        try {
            Map<String, String> params = new HashMap<String, String>();
            params.put("nroAccount", nroAccount);
            String uri = "http://localhost:8090/api/accounts/{nroAccount}";
            AccountDTO account = rest.getForObject(uri, AccountDTO.class, params);
            return Optional.ofNullable(account);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public ResponseEntity<AccountDTO> updateAccount(AccountDTO accountDTO) {
        HttpEntity<AccountDTO> body = new HttpEntity<AccountDTO>(accountDTO);
        String uri = "http://localhost:8090/api/accounts";
        ResponseEntity<AccountDTO> response = rest.exchange(
                uri,
                HttpMethod.PUT,
                body,
                AccountDTO.class);
        return response;
    }

    @Override
    public ResponseEntity<AccountTransactionDTO> pagoDeTerceros(AccountDTO accountDTO) {
        HttpEntity<AccountDTO> body = new HttpEntity<AccountDTO>(accountDTO);
        String uri = "http://localhost:8090/api/accounts/pagoDeTerceros";
        ResponseEntity<AccountTransactionDTO> response = rest.exchange(
                uri,
                HttpMethod.POST,
                body,
                AccountTransactionDTO.class);
        return response;
    }

}
