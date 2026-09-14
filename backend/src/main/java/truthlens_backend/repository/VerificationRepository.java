package truthlens_backend.repository;

import truthlens_backend.model.Verification;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface VerificationRepository extends MongoRepository<Verification, String> {

}