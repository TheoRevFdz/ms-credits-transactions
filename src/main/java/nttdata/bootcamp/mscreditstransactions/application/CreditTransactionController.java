package nttdata.bootcamp.mscreditstransactions.application;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import nttdata.bootcamp.mscreditstransactions.dto.CreditDTO;
import nttdata.bootcamp.mscreditstransactions.dto.CustomerDTO;
import nttdata.bootcamp.mscreditstransactions.enums.TypeTransaction;
import nttdata.bootcamp.mscreditstransactions.interfaces.ICreditService;
import nttdata.bootcamp.mscreditstransactions.interfaces.ICreditTransactionService;
import nttdata.bootcamp.mscreditstransactions.interfaces.ICustomerService;
import nttdata.bootcamp.mscreditstransactions.model.CreditTransaction;

@RestController
public class CreditTransactionController {

    @Autowired
    private ICreditTransactionService service;

    @Autowired
    private ICreditService creditService;

    @Autowired
    private ICustomerService customerService;

    @GetMapping("/{nroCredit}/{type}")
    public ResponseEntity<?> findByNroCreditAndType(@PathVariable String nroCredit, @PathVariable String type) {
        final List<CreditTransaction> response = service.findTransactionByNroCreditAndType(nroCredit, type);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/payment")
    public ResponseEntity<?> createPay(@RequestBody CreditTransaction ct) {
        try {
            Optional<CreditDTO> optCredit = creditService.findCreditByNroCredit(ct.getNroCredit());
            if (optCredit.isPresent()) {
                CreditDTO credit = optCredit.get();
                if (credit.getAmountUsed() == null || credit.getAmountUsed() == 0) {
                    return ResponseEntity.ok(String
                            .format("El credito Nro: %s no presenta deuda de consumos realizados.", ct.getNroCredit()));
                }

                Optional<CustomerDTO> optCustomer = customerService.findCustomerByNroDoc(credit.getNroDoc());
                if (optCustomer.isPresent()) {

                    if (optCustomer.get().getProfileDTO() != null) {
                        List<CreditTransaction> transactions = service
                                .findTransactionsByNroCredit(credit.getNroCredit());

                        if (transactions.size() > optCustomer.get().getProfileDTO().getMaxQuantityTransactions()) {
                            credit.setAmountUsed(
                                    credit.getAmountUsed() + optCustomer.get().getProfileDTO().getCommission());
                        }

                        credit.setAmountUsed(credit.getAmountUsed() - ct.getTransactionAmount());
                        credit.setCreditLine(credit.getCreditLine() + ct.getTransactionAmount());

                        ResponseEntity<?> resp = creditService.updateCredit(credit);
                        if (resp.getStatusCodeValue() == HttpStatus.OK.value()) {
                            ct.setType(TypeTransaction.PAGO.toString());
                            ct.setTransactionDate(new Date());
                            final CreditTransaction response = service.createTransaction(ct);
                            return ResponseEntity.status(HttpStatus.CREATED).body(response);
                        }

                        return ResponseEntity.badRequest()
                                .body(String.format("Error al registrar el %s del Crédito Nro: %s",
                                        TypeTransaction.PAGO.toString(), ct.getNroCredit()));
                    }
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(String.format("No se Perfil: %s", optCustomer.get().getProfile()));
                }
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(String.format("No se encontro cliente con Nro. Documento: %s", credit.getNroDoc()));

            }

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(String.format("No se encontro credito con Nro: %s", ct.getNroCredit()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping("/consume")
    public ResponseEntity<?> createConsume(@RequestBody CreditTransaction ct) {
        try {
            Optional<CreditDTO> optCredit = creditService.findCreditByNroCredit(ct.getNroCredit());

            if (optCredit.isPresent()) {
                CreditDTO credit = optCredit.get();

                if (credit.getCreditLine() < ct.getTransactionAmount()) {
                    return ResponseEntity.badRequest()
                            .body("Linea de crédito insuficiente para completar esta transacción.");
                }
                Optional<CustomerDTO> optCustomer = customerService.findCustomerByNroDoc(credit.getNroDoc());
                if (optCustomer.isPresent()) {

                    if (optCustomer.get().getProfileDTO() != null) {
                        List<CreditTransaction> transactions = service
                                .findTransactionsByNroCredit(credit.getNroCredit());

                        if (transactions.size() > optCustomer.get().getProfileDTO().getMaxQuantityTransactions()) {
                            credit.setAmountUsed(
                                    credit.getAmountUsed() + optCustomer.get().getProfileDTO().getCommission());
                        }

                        Double amountUsed = credit.getAmountUsed() == null ? 0 : credit.getAmountUsed();
                        credit.setAmountUsed(amountUsed + ct.getTransactionAmount());
                        credit.setCreditLine(credit.getCreditLine() - ct.getTransactionAmount());

                        ResponseEntity<?> resp = creditService.updateCredit(credit);
                        if (resp.getStatusCodeValue() == HttpStatus.OK.value()) {
                            ct.setType(TypeTransaction.CONSUMO.toString());
                            ct.setTransactionDate(new Date());
                            final CreditTransaction response = service.createTransaction(ct);
                            return ResponseEntity.status(HttpStatus.CREATED).body(response);
                        }

                        return ResponseEntity.badRequest()
                                .body(String.format("Error al registrar el %s del Crédito Nro: %s",
                                        TypeTransaction.CONSUMO.toString(), ct.getNroCredit()));
                    }
                }
            }

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(String.format("No se encontro credito con Nro: %s", ct.getNroCredit()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
