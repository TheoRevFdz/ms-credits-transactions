package nttdata.bootcamp.mscreditstransactions.application;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import nttdata.bootcamp.mscreditstransactions.dto.AccountDTO;
import nttdata.bootcamp.mscreditstransactions.dto.CreditDTO;
import nttdata.bootcamp.mscreditstransactions.dto.CreditTransactionDTO;
import nttdata.bootcamp.mscreditstransactions.enums.PayStates;
import nttdata.bootcamp.mscreditstransactions.enums.TypeTransaction;
import nttdata.bootcamp.mscreditstransactions.interfaces.IAccountService;
import nttdata.bootcamp.mscreditstransactions.interfaces.ICreditService;
import nttdata.bootcamp.mscreditstransactions.interfaces.ICreditTransactionService;
import nttdata.bootcamp.mscreditstransactions.interfaces.IPaymentScheduleService;
import nttdata.bootcamp.mscreditstransactions.model.CreditTransaction;
import nttdata.bootcamp.mscreditstransactions.model.PaymentSchedule;

@Slf4j
@RestController
public class CreditTransactionController {

    @Autowired
    private ICreditTransactionService service;

    @Autowired
    private ICreditService creditService;

    @Autowired
    private IPaymentScheduleService paymentScheduleService;

    @Autowired
    private IAccountService accountService;

    @CircuitBreaker(name = "credits-transactions", fallbackMethod = "findByNroCreditAndTypeAlt")
    @GetMapping("/{nroCredit}/{type}")
    public ResponseEntity<?> findByNroCreditAndType(@PathVariable String nroCredit, @PathVariable String type) {
        final List<CreditTransaction> response = service.findTransactionByNroCreditAndType(nroCredit, type);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> findByNroCreditAndTypeAlt(@PathVariable String nroCredit, @PathVariable String type,
            Exception ex) {
        log.info(ex.getMessage());
        return ResponseEntity.badRequest().body(new ArrayList<CreditTransaction>());
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

                Optional<AccountDTO> optAccount = accountService.findAccountByNro(ct.getOriginAccount());
                if (!optAccount.isPresent()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(String.format("La cuenta Nro: %s no existe.", ct.getOriginAccount()));
                }

                List<PaymentSchedule> schedules = paymentScheduleService.findByNroCredit(credit.getNroCredit());
                Optional<PaymentSchedule> optPay = schedules.stream().findFirst();
                if (ct.getTransactionAmount() == optPay.get().getMonthlyFee()
                        || ct.getTransactionAmount() == credit.getAmountUsed()) {
                    credit.setAmountUsed(credit.getAmountUsed() - ct.getTransactionAmount());
                    credit.setCreditLine(credit.getCreditLine() + ct.getTransactionAmount());

                    ResponseEntity<?> resp = creditService.updateCredit(credit);
                    if (resp.getStatusCodeValue() == HttpStatus.OK.value()) {
                        ct.setType(TypeTransaction.PAGO.toString());
                        ct.setTransactionDate(new Date());
                        final CreditTransaction response = service.createTransaction(ct);

                        if (optPay.isPresent()) {
                            PaymentSchedule pay = optPay.get();
                            pay.setStatePayFee(PayStates.PAGADO.toString());
                            paymentScheduleService.update(pay);
                        }

                        AccountDTO accountOrigen = optAccount.get();
                        accountOrigen.setAmount(accountOrigen.getAmount() - ct.getTransactionAmount());
                        
                        return ResponseEntity.status(HttpStatus.CREATED).body(response);
                    }
                    return ResponseEntity.badRequest()
                            .body(String.format("Error al registrar el %s del Crédito Nro: %s",
                                    TypeTransaction.PAGO.toString(), ct.getNroCredit()));
                }
                return ResponseEntity.badRequest()
                        .body(String.format("Usted solo puede hacer pagos de los siguientes montos: %s y %s soles.",
                                ct.getTransactionAmount(), credit.getAmountUsed()));
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
                Double amountUsed = credit.getAmountUsed() == null ? 0 : credit.getAmountUsed();
                credit.setAmountUsed(amountUsed + ct.getTransactionAmount());
                credit.setCreditLine(credit.getCreditLine() - ct.getTransactionAmount());

                ResponseEntity<?> resp = creditService.updateCredit(credit);
                if (resp.getStatusCodeValue() == HttpStatus.OK.value()) {
                    ct.setType(TypeTransaction.CONSUMO.toString());
                    ct.setTransactionDate(new Date());
                    final CreditTransaction response = service.createTransaction(ct);

                    List<PaymentSchedule> schedules = new ArrayList<PaymentSchedule>();
                    if (ct.getFeeMonths() == null)
                        ct.setFeeMonths(1.0);
                    double calc = ct.getTransactionAmount() / ct.getFeeMonths();
                    Double fee = Double.parseDouble(String.format("%.2f", calc));
                    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
                    LocalDate nowDate = LocalDate.now();
                    for (int i = 0; i < ct.getFeeMonths(); i++) {
                        final Date payDate = DateUtils.addMonths(fmt.parse(nowDate.toString()), i + 1);
                        PaymentSchedule ps = PaymentSchedule.builder()
                                .nroCredit(credit.getNroCredit())
                                .idTransaction(response.getId())
                                .monthlyFee(fee)
                                .payDateFee(payDate)
                                .statePayFee(PayStates.PENDIENTE.toString())
                                .build();
                        schedules.add(ps);
                    }

                    List<PaymentSchedule> respSchedule = paymentScheduleService.createFromList(schedules);
                    CreditTransactionDTO respDTO = CreditTransactionDTO.builder()
                            .creditTransaction(response)
                            .paymentSchedules(respSchedule)
                            .build();

                    return ResponseEntity.status(HttpStatus.CREATED).body(respDTO);
                }
                return ResponseEntity.badRequest()
                        .body(String.format("Error al registrar el %s del Crédito Nro: %s",
                                TypeTransaction.CONSUMO.toString(), ct.getNroCredit()));
            }

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(String.format("No se encontro credito con Nro: %s", ct.getNroCredit()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/validateFee/{nroDoc}")
    public ResponseEntity<?> validateCuotasVencidas(@PathVariable String nroDoc) {
        List<CreditDTO> credits = creditService.findCreditsByNroDoc(nroDoc);

        boolean resp = false;
        for (CreditDTO c : credits) {
            List<PaymentSchedule> schedules = paymentScheduleService.findByNroCredit(c.getNroCredit());
            List<PaymentSchedule> pendientes = schedules.stream().map(s -> {
                if (s.getPayDateFee().before(new Date())) {
                    return s;
                }
                return null;
            }).collect(Collectors.toList());

            pendientes = pendientes.stream().filter(s -> s != null).collect(Collectors.toList());

            if (pendientes.size() > 0) {
                resp = true;
                break;
            }
        }

        return ResponseEntity.ok().body(resp);
    }
}
