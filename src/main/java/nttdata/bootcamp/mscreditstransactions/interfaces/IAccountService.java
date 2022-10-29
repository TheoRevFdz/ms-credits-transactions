package nttdata.bootcamp.mscreditstransactions.interfaces;

import java.util.Optional;

import nttdata.bootcamp.mscreditstransactions.dto.AccountDTO;

public interface IAccountService {
    public Optional<AccountDTO> findAccountByNro(String nroAccount);

    public AccountDTO updateAccount(AccountDTO accountDTO);
}
