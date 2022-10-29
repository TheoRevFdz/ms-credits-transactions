package nttdata.bootcamp.mscreditstransactions.interfaces;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;

import nttdata.bootcamp.mscreditstransactions.config.RestConfig;
import nttdata.bootcamp.mscreditstransactions.dto.AccountDTO;

@Service
public class AccountServiceImpl implements IAccountService {
    @Autowired
    private RestConfig rest;

    @Override
    public Optional<AccountDTO> findAccountByNro(String nroAccount) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("nroAccount", nroAccount);
        String uri = "http://localhost:8090/accounts/{nroAccount}";
        AccountDTO account = rest.getForObject(uri, AccountDTO.class, params);
        return Optional.ofNullable(account);
    }

    @Override
    public AccountDTO updateAccount(AccountDTO accountDTO) {
        HttpEntity<AccountDTO> body = new HttpEntity<AccountDTO>(accountDTO);
        return null;
    }

}
