package nttdata.bootcamp.mscreditstransactions.infraestructure;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

import nttdata.bootcamp.mscreditstransactions.model.CreditTransaction;

@Repository
public interface ICreditTransactionReactiveRepository extends ReactiveMongoRepository<CreditTransaction,String>{
    
}
